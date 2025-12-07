package com.taichu.application.service;

import com.alibaba.cola.dto.MultiResponse;
import com.alibaba.cola.dto.SingleResponse;
import com.taichu.application.annotation.EntranceLog;
import com.taichu.application.executor.SingleStoryboardVideoTaskExecutor;
import com.taichu.application.executor.StoryboardVideoTaskExecutor;
import com.taichu.application.helper.WorkflowValidationHelper;
import com.taichu.common.common.exception.AppServiceExceptionHandle;
import com.taichu.common.common.util.StreamUtil;
import com.taichu.domain.algo.gateway.FileGateway;
import com.taichu.domain.enums.*;
import com.taichu.domain.model.FicAlgoTaskBO;
import com.taichu.domain.model.FicResourceBO;
import com.taichu.domain.model.FicStoryboardBO;
import com.taichu.domain.model.FicWorkflowTaskBO;
import com.taichu.infra.repo.FicAlgoTaskRepository;
import com.taichu.infra.repo.FicResourceRepository;
import com.taichu.infra.repo.FicStoryboardRepository;
import com.taichu.infra.repo.FicWorkflowTaskRepository;
import com.taichu.sdk.model.StoryboardWorkflowTaskStatusDTO;
import com.taichu.sdk.model.VideoListItemDTO;
import com.taichu.sdk.model.request.GenerateVideoRequest;
import com.taichu.sdk.model.request.SingleStoryboardVideoRegenRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
public class StoryboardVideoAppService {

    @Autowired
    private WorkflowValidationHelper workflowValidationHelper;

    @Autowired
    private FicWorkflowTaskRepository ficWorkflowTaskRepository;

    @Autowired
    private FicAlgoTaskRepository ficAlgoTaskRepository;

    @Autowired
    private FicStoryboardRepository ficStoryboardRepository;

    @Autowired
    private FicResourceRepository ficResourceRepository;

    @Autowired
    private StoryboardVideoTaskExecutor storyboardVideoTaskExecutor;

    @Autowired
    private SingleStoryboardVideoTaskExecutor singleStoryboardVideoTaskExecutor;

    @Autowired
    private FileGateway fileGateway;

    /**
     * 提交视频生成任务
     */
    @EntranceLog(bizCode = "生成分镜视频")
    @AppServiceExceptionHandle(biz = "生成分镜视频")
    public SingleResponse<Long> submitGenVideoTask(GenerateVideoRequest request, Long userId) {
        // 1. 校验工作流状态
        SingleResponse<?> validateResponse = workflowValidationHelper.validateWorkflow(
                request.getWorkflowId(), userId, WorkflowStatusEnum.STORYBOARD_IMG_GEN_DONE);
        if (!validateResponse.isSuccess()) {
            return SingleResponse.buildFailure(validateResponse.getErrCode(), validateResponse.getErrMessage());
        }

        // 2. 创建任务
        return storyboardVideoTaskExecutor.submitTask(request.getWorkflowId(), request);
    }

    /**
     * 查询任务状态
     */
    @EntranceLog(bizCode = "查询分镜视频任务进度")
    @AppServiceExceptionHandle(biz = "查询分镜视频任务进度")
    public SingleResponse<StoryboardWorkflowTaskStatusDTO> getVideoTaskStatus(Long taskId) {
        FicWorkflowTaskBO ficWorkflowTaskBO = ficWorkflowTaskRepository.findById(taskId);
        if (ficWorkflowTaskBO == null) {
            return SingleResponse.buildFailure("", "taskId不存在");
        }
        if (!TaskTypeEnum.STORYBOARD_VIDEO_GENERATION.name().equals(ficWorkflowTaskBO.getTaskType())
                && !TaskTypeEnum.USER_RETRY_SINGLE_STORYBOARD_VIDEO_GENERATION.name().equals(ficWorkflowTaskBO.getTaskType())
        ) {
            return SingleResponse.buildFailure("", "不是视频生成任务");
        }

        Long workflowId = ficWorkflowTaskBO.getWorkflowId();

        List<FicAlgoTaskBO> ficAlgoTaskBOList = ficAlgoTaskRepository.findByWorkflowTaskId(ficWorkflowTaskBO.getId());
        StoryboardWorkflowTaskStatusDTO taskStatusDTO = new StoryboardWorkflowTaskStatusDTO();

        List<FicStoryboardBO> allStoryboards = ficStoryboardRepository.findValidByWorkflowId(workflowId);

        if (TaskStatusEnum.FAILED.getCode().equals(ficWorkflowTaskBO.getStatus())) {
            taskStatusDTO.setTaskId(taskId);
            taskStatusDTO.setStatus(TaskStatusEnum.FAILED.name());
            return SingleResponse.of(taskStatusDTO);
        } else if (TaskStatusEnum.COMPLETED.getCode().equals(ficWorkflowTaskBO.getStatus())) {
            // 处理完成状态（包括部分成功的情况）
            // 容错处理：过滤null值和无效数据，确保即使部分任务失败也能正确统计
            List<Long> completedVideoIdList = StreamUtil.toStream(ficAlgoTaskBOList)
                    .filter(t -> t != null 
                            && TaskStatusEnum.COMPLETED.getCode().equals(t.getStatus())
                            && t.getRelevantId() != null)
                    .map(FicAlgoTaskBO::getRelevantId)
                    .distinct()
                    .collect(Collectors.toList());

            taskStatusDTO.setTaskId(taskId);
            taskStatusDTO.setStatus(TaskStatusEnum.COMPLETED.name());
            taskStatusDTO.setCompleteCnt(completedVideoIdList.size());
            taskStatusDTO.setCompletedStoryboardIds(completedVideoIdList);
            taskStatusDTO.setTotalCnt(allStoryboards != null ? allStoryboards.size() : completedVideoIdList.size());
        } else {
            // 处理运行中状态
            // 容错处理：过滤null值和无效数据
            List<Long> completedVideoIdList = StreamUtil.toStream(ficAlgoTaskBOList)
                    .filter(t -> t != null 
                            && TaskStatusEnum.COMPLETED.getCode().equals(t.getStatus())
                            && t.getRelevantId() != null)
                    .map(FicAlgoTaskBO::getRelevantId)
                    .distinct()
                    .collect(Collectors.toList());

            taskStatusDTO.setTaskId(taskId);
            taskStatusDTO.setStatus(TaskStatusEnum.RUNNING.name());
            taskStatusDTO.setCompleteCnt(completedVideoIdList.size());
            taskStatusDTO.setCompletedStoryboardIds(completedVideoIdList);
            taskStatusDTO.setTotalCnt(allStoryboards != null ? allStoryboards.size() : 0);
        }

        return SingleResponse.of(taskStatusDTO);
    }

    /**
     * 获取单个视频信息
     * 
     * @param storyboardId 分镜ID
     * @return 包含视频信息的响应对象，包括视频资源URL和分镜图片缩略图URL
     */
    @EntranceLog(bizCode = "获取单个视频信息")
    @AppServiceExceptionHandle(biz = "获取单个视频信息")
    public SingleResponse<VideoListItemDTO> getSingleVideo(Long storyboardId) {
        // 1. 根据分镜ID查询分镜信息
        FicStoryboardBO ficStoryboardBO = ficStoryboardRepository.findById(storyboardId);
        if (ficStoryboardBO == null) {
            return SingleResponse.buildFailure("", "not exist storyboardId");
        }

        // 2. 查询对应的视频资源
        List<FicResourceBO> videoResourceList = ficResourceRepository.findValidByWorkflowIdAndResourceType(
                ficStoryboardBO.getWorkflowId(), ResourceTypeEnum.STORYBOARD_VIDEO);

        FicResourceBO storyBoardVideoResourceBO = StreamUtil.toStream(videoResourceList)
                .filter(t -> ficStoryboardBO.getId().equals(t.getRelevanceId()))
                .findFirst()
                .orElse(null);

        if (storyBoardVideoResourceBO == null) {
            return SingleResponse.buildFailure("", "视频未生成");
        }

        // 3. 查询对应的分镜图片资源作为缩略图
        List<FicResourceBO> imgResourceList = ficResourceRepository.findValidByWorkflowIdAndResourceType(
                ficStoryboardBO.getWorkflowId(), ResourceTypeEnum.STORYBOARD_IMG);

        FicResourceBO storyBoardImgResourceBO = StreamUtil.toStream(imgResourceList)
                .filter(t -> ficStoryboardBO.getId().equals(t.getRelevanceId()))
                .findFirst()
                .orElse(null);

        // 4. 构建视频信息DTO并返回
        return SingleResponse.of(buildVideoListItemDTO(ficStoryboardBO, storyBoardVideoResourceBO, storyBoardImgResourceBO));
    }

    /**
     * 获取所有视频信息
     * 
     * @param workflowId 工作流ID
     * @return 包含所有视频信息的响应对象列表，每个视频包含视频资源URL和分镜图片缩略图URL
     */
    @EntranceLog(bizCode = "获取所有视频信息")
    @AppServiceExceptionHandle(biz = "获取所有视频信息")
    public MultiResponse<VideoListItemDTO> getAllVideo(Long workflowId) {
        // 1. 根据工作流ID查询所有分镜信息
        List<FicStoryboardBO> ficStoryboardBOList = ficStoryboardRepository.findValidByWorkflowId(workflowId);
        if (ficStoryboardBOList.isEmpty()) {
            return MultiResponse.buildSuccess();
        }

        // 2. 批量查询所有视频资源
        List<FicResourceBO> videoResourceList = ficResourceRepository.findValidByWorkflowIdAndResourceType(
                workflowId, ResourceTypeEnum.STORYBOARD_VIDEO);

        // 3. 批量查询所有分镜图片资源
        List<FicResourceBO> imgResourceList = ficResourceRepository.findValidByWorkflowIdAndResourceType(
                workflowId, ResourceTypeEnum.STORYBOARD_IMG);

        // 4. 构建资源映射，提高查询效率，避免N+1查询问题
        Map<Long, FicResourceBO> videoResourceMap = StreamUtil.toStream(videoResourceList)
                .collect(Collectors.toMap(FicResourceBO::getRelevanceId, resource -> resource));
        
        Map<Long, FicResourceBO> imgResourceMap = StreamUtil.toStream(imgResourceList)
                .collect(Collectors.toMap(FicResourceBO::getRelevanceId, resource -> resource));

        // 5. 为每个分镜构建视频信息DTO（容错处理：只返回有视频资源的分镜，即使部分分镜失败也不会崩溃）
        List<VideoListItemDTO> videoList = StreamUtil.toStream(ficStoryboardBOList)
                .filter(ficStoryboardBO -> ficStoryboardBO != null && ficStoryboardBO.getId() != null)
                .map(ficStoryboardBO -> {
                    FicResourceBO videoResourceBO = videoResourceMap.get(ficStoryboardBO.getId());
                    FicResourceBO imgResourceBO = imgResourceMap.get(ficStoryboardBO.getId());
                    return buildVideoListItemDTO(ficStoryboardBO, videoResourceBO, imgResourceBO);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return MultiResponse.of(videoList);
    }

    /**
     * 重新生成单个视频
     */
    @EntranceLog(bizCode = "重新生成单个视频")
    @AppServiceExceptionHandle(biz = "重新生成单个视频")
    public SingleResponse<Long> regenerateSingleVideo(SingleStoryboardVideoRegenRequest request, Long userId) {
        // 1. 校验工作流状态
        SingleResponse<?> validateResponse = workflowValidationHelper.validateWorkflow(
                request.getWorkflowId(), userId, WorkflowStatusEnum.STORYBOARD_VIDEO_GEN_DONE);
        if (!validateResponse.isSuccess()) {
            return SingleResponse.buildFailure(validateResponse.getErrCode(), validateResponse.getErrMessage());
        }

        // 2. 创建任务
        return singleStoryboardVideoTaskExecutor.submitTask(request.getWorkflowId(), request);
    }

    /**
     * 构建视频列表项DTO
     * 
     * @param ficStoryboardBO 分镜业务对象
     * @param videoResourceBO 视频资源对象，可为null
     * @param imgResourceBO 分镜图片资源对象，可为null
     * @return 视频列表项DTO，如果视频资源不存在则返回null
     */
    private VideoListItemDTO buildVideoListItemDTO(FicStoryboardBO ficStoryboardBO, FicResourceBO videoResourceBO, FicResourceBO imgResourceBO) {
        if (videoResourceBO == null) {
            return null;
        }

        VideoListItemDTO videoListItemDTO = new VideoListItemDTO();
        videoListItemDTO.setStoryboardId(ficStoryboardBO.getId());
        videoListItemDTO.setStoryboardResourceId(videoResourceBO.getId());
        videoListItemDTO.setOrderIndex(ficStoryboardBO.getOrderIndex());

        // 设置缩略图URL：优先使用分镜图片，如果不存在则置空
        if (imgResourceBO != null) {
            String thumbnailUrl = fileGateway.getFileUrl(imgResourceBO.getResourceUrl()).getData();
            videoListItemDTO.setThumbnailUrl(thumbnailUrl);
        } else {
            // 分镜图片不存在，缩略图置空
            videoListItemDTO.setThumbnailUrl(null);
        }

        // 设置视频资源URL
        String videoResourceUrl = fileGateway.getFileUrl(videoResourceBO.getResourceUrl()).getData();
        videoListItemDTO.setVideoResourceUrl(videoResourceUrl);

        // 设置分镜内容
        videoListItemDTO.setStoryboardContent(ficStoryboardBO.getContent());

        return videoListItemDTO;
    }

    /**
     * 获取视频资源
     */
    public Resource getVideoResource(Long resourceId) {
        FicResourceBO ficResourceBO = ficResourceRepository.findById(resourceId);
        if (ficResourceBO == null) {
            log.error("Resource not found, resourceId: {}", resourceId);
            return null;
        }

        try {
            InputStream inputStream = fileGateway.getFileStream(ficResourceBO.getResourceUrl());
            return new InputStreamResource(inputStream);
        } catch (Exception e) {
            log.error("Failed to get video resource: {}", ficResourceBO.getResourceUrl(), e);
            return null;
        }
    }
}