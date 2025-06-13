package com.taichu.application.service;

import com.alibaba.cola.dto.MultiResponse;
import com.alibaba.cola.dto.SingleResponse;
import com.taichu.application.helper.WorkflowValidationHelper;
import com.taichu.common.common.util.StreamUtil;
import com.taichu.domain.algo.gateway.FileGateway;
import com.taichu.domain.enums.*;
import com.taichu.domain.model.FicAlgoTaskBO;
import com.taichu.domain.model.FicResourceBO;
import com.taichu.domain.model.FicWorkflowTaskBO;
import com.taichu.infra.repo.FicAlgoTaskRepository;
import com.taichu.infra.repo.FicResourceRepository;
import com.taichu.infra.repo.FicWorkflowTaskRepository;
import com.taichu.sdk.model.FullVideoListItemDTO;
import com.taichu.sdk.model.WorkflowTaskStatusDTO;
import com.taichu.sdk.model.request.ComposeVideoRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private WorkflowValidationHelper workflowValidationHelper;

    @Autowired
    private FicWorkflowTaskRepository ficWorkflowTaskRepository;
    
    @Autowired
    private FicResourceRepository ficResourceRepository;

    @Autowired
    private FileGateway fileGateway;

    /**
     * 提交视频合成任务
     */
    public SingleResponse<Long> submitComposeVideoTask(ComposeVideoRequest request, Long userId) {
        // 1. 校验工作流状态
        SingleResponse<?> validateResponse = workflowValidationHelper.validateWorkflow(
                request.getWorkflowId(), userId, WorkflowStatusEnum.STORYBOARD_VIDEO_GEN_DONE);
        if (!validateResponse.isSuccess()) {
            return SingleResponse.buildFailure(validateResponse.getErrCode(), validateResponse.getErrMessage());
        }

        // 2. 创建任务
        FicWorkflowTaskBO ficWorkflowTaskBO = new FicWorkflowTaskBO();
        ficWorkflowTaskBO.setWorkflowId(request.getWorkflowId());
        ficWorkflowTaskBO.setTaskType(TaskTypeEnum.FULL_VIDEO_GENERATION.name());
        ficWorkflowTaskBO.setStatus(TaskStatusEnum.RUNNING.getCode());
        ficWorkflowTaskBO.setGmtCreate(System.currentTimeMillis());
        long workflowTaskId = ficWorkflowTaskRepository.createFicWorkflowTask(ficWorkflowTaskBO);
        ficWorkflowTaskBO.setId(workflowTaskId);

        return SingleResponse.of(workflowTaskId);
    }

    /**
     * 查询任务状态
     */
    public SingleResponse<WorkflowTaskStatusDTO> getComposeTaskStatus(Long taskId) {
        FicWorkflowTaskBO ficWorkflowTaskBO = ficWorkflowTaskRepository.findById(taskId);
        if (ficWorkflowTaskBO == null) {
            return SingleResponse.buildFailure("", "taskId不存在");
        }
        if (!TaskTypeEnum.FULL_VIDEO_GENERATION.name().equals(ficWorkflowTaskBO.getTaskType())) {
            return SingleResponse.buildFailure("", "不是视频合成任务");
        }

        WorkflowTaskStatusDTO taskStatusDTO = new WorkflowTaskStatusDTO();
        taskStatusDTO.setTaskId(taskId);
        taskStatusDTO.setStatus(TaskStatusEnum.fromCode(ficWorkflowTaskBO.getStatus()).name());

        return SingleResponse.of(taskStatusDTO);
    }

    /**
     * 获取合成视频信息
     */
    public MultiResponse<FullVideoListItemDTO> getComposeVideo(Long workflowId) {
        List<FicResourceBO> ficResourceBOList = ficResourceRepository.findByWorkflowIdAndResourceType(
                workflowId, ResourceTypeEnum.FULL_VIDEO);
        
        if (ficResourceBOList.isEmpty()) {
            return MultiResponse.buildSuccess();
        }

        List<FullVideoListItemDTO> videoList = StreamUtil.toStream(ficResourceBOList)
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return MultiResponse.of(videoList);
    }

    /**
     * 下载合成视频
     */
    public Optional<Resource> downloadComposeVideo(Long workflowId) {
        List<FicResourceBO> ficResourceBOList = ficResourceRepository.findByWorkflowIdAndResourceType(
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

    private FullVideoListItemDTO convertToDTO(FicResourceBO ficResourceBO) {
        FullVideoListItemDTO videoListItemDTO = new FullVideoListItemDTO();
        videoListItemDTO.setWorkflowId(ficResourceBO.getWorkflowId());
        videoListItemDTO.setStoryboardResourceId(ficResourceBO.getId());
        videoListItemDTO.setThumbnailUrl(ficResourceBO.getResourceUrl());
        return videoListItemDTO;
    }
}