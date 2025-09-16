package com.taichu.application.service;

import com.alibaba.cola.dto.MultiResponse;
import com.alibaba.cola.dto.SingleResponse;
import com.taichu.application.annotation.EntranceLog;
import com.taichu.application.executor.ComposeVideoTaskExecutor;
import com.taichu.application.executor.RetryComposeVideoTaskExecutor;
import com.taichu.application.helper.WorkflowValidationHelper;
import com.taichu.common.common.exception.AppServiceExceptionHandle;
import com.taichu.common.common.util.StreamUtil;
import com.taichu.domain.algo.gateway.FileGateway;
import com.taichu.domain.enums.*;
import com.taichu.domain.model.FicResourceBO;
import com.taichu.domain.model.FicWorkflowTaskBO;
import com.taichu.infra.repo.FicResourceRepository;
import com.taichu.infra.repo.FicWorkflowTaskRepository;
import com.taichu.sdk.model.FullVideoListItemDTO;
import com.taichu.sdk.model.WorkflowTaskStatusDTO;
import com.taichu.sdk.model.request.ComposeVideoRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ComposeVideoAppService {

    private final WorkflowValidationHelper workflowValidationHelper;
    private final FicWorkflowTaskRepository ficWorkflowTaskRepository;
    private final FicResourceRepository ficResourceRepository;
    private final FileGateway fileGateway;
    private final ComposeVideoTaskExecutor composeVideoTaskExecutor;
    private final RetryComposeVideoTaskExecutor retryComposeVideoTaskExecutor;

    public ComposeVideoAppService(WorkflowValidationHelper workflowValidationHelper, FicWorkflowTaskRepository ficWorkflowTaskRepository, FicResourceRepository ficResourceRepository, FileGateway fileGateway, ComposeVideoTaskExecutor composeVideoTaskExecutor, RetryComposeVideoTaskExecutor retryComposeVideoTaskExecutor) {
        this.workflowValidationHelper = workflowValidationHelper;
        this.ficWorkflowTaskRepository = ficWorkflowTaskRepository;
        this.ficResourceRepository = ficResourceRepository;
        this.fileGateway = fileGateway;
        this.composeVideoTaskExecutor = composeVideoTaskExecutor;
        this.retryComposeVideoTaskExecutor = retryComposeVideoTaskExecutor;
    }


    /**
     * 提交视频合成任务
     */
    @EntranceLog(bizCode = "提交视频合成任务")
    @AppServiceExceptionHandle(biz = "提交视频合成任务")
    public SingleResponse<Long> submitComposeVideoTask(ComposeVideoRequest request, Long userId) {
        // 1. 校验工作流状态
        SingleResponse<?> validateResponse = workflowValidationHelper.validateWorkflow(
                request.getWorkflowId(), userId, WorkflowStatusEnum.STORYBOARD_VIDEO_GEN_DONE);
        if (!validateResponse.isSuccess()) {
            return SingleResponse.buildFailure(validateResponse.getErrCode(), validateResponse.getErrMessage());
        }

        // 2. 提交任务
        return composeVideoTaskExecutor.submitTask(request.getWorkflowId(), request);
    }

    /**
     * 用户重新提交视频合成任务
     * @param request
     * @param userId
     * @return
     */
    @EntranceLog(bizCode = "用户重新提交视频合成任务")
    @AppServiceExceptionHandle(biz = "用户重新提交视频合成任务")
    public SingleResponse<Long> submitReComposeVideoTask(ComposeVideoRequest request, Long userId) {
        // 1. 校验工作流状态
        SingleResponse<?> validateResponse = workflowValidationHelper.validateWorkflow(
                request.getWorkflowId(), userId, WorkflowStatusEnum.FULL_VIDEO_GEN_DONE);
        if (!validateResponse.isSuccess()) {
            return SingleResponse.buildFailure(validateResponse.getErrCode(), validateResponse.getErrMessage());
        }

        // 2. 提交任务
        return retryComposeVideoTaskExecutor.submitTask(request.getWorkflowId(), request);
    }

    /**
     * 查询任务状态
     */
    @EntranceLog(bizCode = "查询合成视频任务状态")
    @AppServiceExceptionHandle(biz = "查询合成视频任务状态")
    public SingleResponse<WorkflowTaskStatusDTO> getComposeTaskStatus(Long taskId) {
        FicWorkflowTaskBO ficWorkflowTaskBO = ficWorkflowTaskRepository.findById(taskId);
        if (ficWorkflowTaskBO == null) {
            return SingleResponse.buildFailure("", "taskId不存在");
        }
        if (!TaskTypeEnum.FULL_VIDEO_GENERATION.name().equals(ficWorkflowTaskBO.getTaskType())
                && !TaskTypeEnum.USER_RETRY_FULL_VIDEO_GENERATION.name().equals(ficWorkflowTaskBO.getTaskType())
        ) {
            return SingleResponse.buildFailure("", "不是视频合成任务");
        }

        WorkflowTaskStatusDTO taskStatusDTO = new WorkflowTaskStatusDTO();
        taskStatusDTO.setTaskId(taskId);
        taskStatusDTO.setStatus(TaskStatusEnum.fromCode(ficWorkflowTaskBO.getStatus()).name());

        return SingleResponse.of(taskStatusDTO);
    }

    /**
     * 获取合成视频信息
     * 
     * @param workflowId 工作流ID
     * @return 包含合成视频信息的响应对象列表，包括视频资源URL和分镜图片缩略图URL
     */
    @EntranceLog(bizCode = "获取合成视频信息")
    @AppServiceExceptionHandle(biz = "获取合成视频信息")
    public MultiResponse<FullVideoListItemDTO> getComposeVideo(Long workflowId) {
        // 1. 查询合成视频资源
        List<FicResourceBO> videoResourceList = ficResourceRepository.findValidByWorkflowIdAndResourceType(
                workflowId, ResourceTypeEnum.FULL_VIDEO);
        
        if (videoResourceList.isEmpty()) {
            return MultiResponse.buildFailure("", "合成视频资源不存在");
        }

        // 2. 查询第一个分镜图片资源作为缩略图
        List<FicResourceBO> imgResourceList = ficResourceRepository.findValidByWorkflowIdAndResourceType(
                workflowId, ResourceTypeEnum.STORYBOARD_IMG);
        
        FicResourceBO thumbnailResourceBO = imgResourceList.isEmpty() ? null : imgResourceList.get(0);

        // 3. 构建视频信息DTO列表
        List<FullVideoListItemDTO> videoList = StreamUtil.toStream(videoResourceList)
                .map(videoResourceBO -> buildFullVideoListItemDTO(videoResourceBO, thumbnailResourceBO))
                .collect(Collectors.toList());

        return MultiResponse.of(videoList);
    }

    /**
     * 下载合成视频
     */
    public Optional<Resource> downloadComposeVideo(Long workflowId) {
        List<FicResourceBO> ficResourceBOList = ficResourceRepository.findValidByWorkflowIdAndResourceType(
                workflowId, ResourceTypeEnum.FULL_VIDEO);
        if (ficResourceBOList.isEmpty()) {
            return Optional.empty();
        }

        try {
            // 获取最新的合成视频
            FicResourceBO latestVideo = ficResourceBOList.get(0);
            InputStream inputStream = fileGateway.getFileStream(latestVideo.getResourceUrl());
            return Optional.of(new InputStreamResource(inputStream));
        } catch (Exception e) {
            log.error("Failed to get compose video for workflow: {}", workflowId, e);
            return Optional.empty();
        }
    }

    /**
     * 构建完整视频列表项DTO
     * 
     * @param videoResourceBO 视频资源对象
     * @param thumbnailResourceBO 缩略图资源对象，可为null
     * @return 完整视频列表项DTO
     */
    private FullVideoListItemDTO buildFullVideoListItemDTO(FicResourceBO videoResourceBO, FicResourceBO thumbnailResourceBO) {
        FullVideoListItemDTO videoListItemDTO = new FullVideoListItemDTO();
        videoListItemDTO.setWorkflowId(videoResourceBO.getWorkflowId());
        videoListItemDTO.setStoryboardResourceId(videoResourceBO.getId());
        
        // 设置缩略图URL：使用第一个分镜图片，如果不存在则置空
        if (thumbnailResourceBO != null) {
            String thumbnailUrl = fileGateway.getFileUrl(thumbnailResourceBO.getResourceUrl()).getData();
            videoListItemDTO.setThumbnailUrl(thumbnailUrl);
        } else {
            videoListItemDTO.setThumbnailUrl(null);
        }
        
        // 设置视频资源URL
        String videoResourceUrl = fileGateway.getFileUrl(videoResourceBO.getResourceUrl()).getData();
        videoListItemDTO.setVideoResourceUrl(videoResourceUrl);
        
        return videoListItemDTO;
    }
}