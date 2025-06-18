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
        if (!TaskTypeEnum.STORYBOARD_VIDEO_GENERATION.name().equals(ficWorkflowTaskBO.getTaskType())) {
            return SingleResponse.buildFailure("", "不是视频生成任务");
        }

        List<FicAlgoTaskBO> ficAlgoTaskBOList = ficAlgoTaskRepository.findByWorkflowTaskId(ficWorkflowTaskBO.getId());
        StoryboardWorkflowTaskStatusDTO taskStatusDTO = new StoryboardWorkflowTaskStatusDTO();

        if (TaskStatusEnum.FAILED.getCode().equals(ficWorkflowTaskBO.getStatus())) {
            return SingleResponse.buildFailure("", "视频生成任务失败");
        } else if (TaskStatusEnum.COMPLETED.getCode().equals(ficWorkflowTaskBO.getStatus())) {
            List<Long> completedVideoIdList = StreamUtil.toStream(ficAlgoTaskBOList)
                    .filter(t -> TaskStatusEnum.COMPLETED.getCode().equals(t.getStatus()))
                    .map(FicAlgoTaskBO::getRelevantId)
                    .collect(Collectors.toList());

            taskStatusDTO.setTaskId(taskId);
            taskStatusDTO.setStatus(TaskStatusEnum.COMPLETED.name());
            taskStatusDTO.setCompleteCnt(ficAlgoTaskBOList.size());
            taskStatusDTO.setTotalCnt(ficAlgoTaskBOList.size());
            taskStatusDTO.setCompletedStoryboardIds(completedVideoIdList);
        } else {
            List<Long> completedVideoIdList = StreamUtil.toStream(ficAlgoTaskBOList)
                    .filter(t -> TaskStatusEnum.COMPLETED.getCode().equals(t.getStatus()))
                    .map(FicAlgoTaskBO::getRelevantId)
                    .collect(Collectors.toList());

            taskStatusDTO.setTaskId(taskId);
            taskStatusDTO.setStatus(TaskStatusEnum.RUNNING.name());
            taskStatusDTO.setCompleteCnt(completedVideoIdList.size());
            taskStatusDTO.setTotalCnt(ficAlgoTaskBOList.size());
            taskStatusDTO.setCompletedStoryboardIds(completedVideoIdList);
        }

        return SingleResponse.of(taskStatusDTO);
    }

    /**
     * 获取单个视频信息
     */
    @EntranceLog(bizCode = "获取单个视频信息")
    @AppServiceExceptionHandle(biz = "获取单个视频信息")
    public SingleResponse<VideoListItemDTO> getSingleVideo(Long storyboardId) {
        FicStoryboardBO ficStoryboardBO = ficStoryboardRepository.findById(storyboardId);
        if (ficStoryboardBO == null) {
            return SingleResponse.buildFailure("", "not exist storyboardId");
        }

        List<FicResourceBO> ficResourceBOList = ficResourceRepository.findByWorkflowIdAndResourceType(
                ficStoryboardBO.getWorkflowId(), ResourceTypeEnum.STORYBOARD_VIDEO);

        FicResourceBO ficResourceBO = StreamUtil.toStream(ficResourceBOList)
                .filter(t -> ficStoryboardBO.getId().equals(t.getRelevanceId()))
                .findFirst()
                .orElse(null);

        if (ficResourceBO == null) {
            return SingleResponse.buildFailure("", "视频未生成");
        }

        return SingleResponse.of(convertToDTO(ficStoryboardBO, ficResourceBO));
    }

    /**
     * 获取所有视频信息
     */
    @EntranceLog(bizCode = "获取所有视频信息")
    @AppServiceExceptionHandle(biz = "获取所有视频信息")
    public MultiResponse<VideoListItemDTO> getAllVideo(Long workflowId) {
        List<FicStoryboardBO> ficStoryboardBOList = ficStoryboardRepository.findByWorkflowId(workflowId);
        if (ficStoryboardBOList.isEmpty()) {
            return MultiResponse.buildSuccess();
        }

        List<FicResourceBO> ficResourceBOList = ficResourceRepository.findByWorkflowIdAndResourceType(
                workflowId, ResourceTypeEnum.STORYBOARD_VIDEO);

        List<VideoListItemDTO> videoList = StreamUtil.toStream(ficStoryboardBOList)
                .map(ficStoryboardBO -> {
                    FicResourceBO ficResourceBO = StreamUtil.toStream(ficResourceBOList)
                            .filter(t -> ficStoryboardBO.getId().equals(t.getRelevanceId()))
                            .findFirst()
                            .orElse(null);
                    return convertToDTO(ficStoryboardBO, ficResourceBO);
                })
                .filter(t -> t != null)
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

    private VideoListItemDTO convertToDTO(FicStoryboardBO ficStoryboardBO, FicResourceBO ficResourceBO) {
        if (ficResourceBO == null) {
            return null;
        }

        VideoListItemDTO videoListItemDTO = new VideoListItemDTO();
        videoListItemDTO.setStoryboardId(ficStoryboardBO.getId());
        videoListItemDTO.setStoryboardResourceId(ficResourceBO.getId());
        videoListItemDTO.setThumbnailUrl(ficResourceBO.getResourceUrl());
        videoListItemDTO.setOrderIndex(ficStoryboardBO.getOrderIndex());
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