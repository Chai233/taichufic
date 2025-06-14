package com.taichu.application.service;

import com.alibaba.cola.dto.MultiResponse;
import com.alibaba.cola.dto.SingleResponse;
import com.taichu.application.executor.StoryboardImgTaskExecutor;
import com.taichu.application.executor.SingleStoryboardImgTaskExecutor;
import com.taichu.application.helper.WorkflowValidationHelper;
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
import com.taichu.sdk.model.StoryboardImgListItemDTO;
import com.taichu.sdk.model.StoryboardWorkflowTaskStatusDTO;
import com.taichu.sdk.model.request.GenerateStoryboardRequest;
import com.taichu.sdk.model.request.SingleStoryboardRegenRequest;
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
public class StoryboardImgAppService {

    @Autowired
    private WorkflowValidationHelper workflowValidationHelper;

    @Autowired
    private StoryboardImgTaskExecutor storyboardTaskExecutor;

    @Autowired
    private SingleStoryboardImgTaskExecutor singleStoryboardImgTaskExecutor;

    @Autowired
    private FicWorkflowTaskRepository ficWorkflowTaskRepository;
    @Autowired
    private FicAlgoTaskRepository ficAlgoTaskRepository;
    @Autowired
    private FicStoryboardRepository ficStoryboardRepository;
    @Autowired
    private FicResourceRepository ficResourceRepository;

    @Autowired
    private FileGateway fileGateway;

    /**
     * 获取分镜图片资源
     * @param resourceId 资源ID
     * @return 图片资源
     */
    public Resource getStoryboardResource(Long resourceId) {
        FicResourceBO ficResourceBO = ficResourceRepository.findById(resourceId);
        if (ficResourceBO == null) {
            log.error("Resource not found, resourceId: {}", resourceId);
            return null;
        }

        try {
            InputStream inputStream = fileGateway.getFileStream(ficResourceBO.getResourceUrl());
            return new InputStreamResource(inputStream);
        } catch (Exception e) {
            log.error("Failed to get storyboard resource: {}", ficResourceBO.getResourceUrl(), e);
            return null;
        }
    }

    /**
     * 提交分镜图生成任务
     * @param request
     * @param userId
     * @return
     */
    public SingleResponse<Long> submitGenStoryboardTask(GenerateStoryboardRequest request, Long userId) {
        // 1. 校验工作流状态
        SingleResponse<?> validateResponse = workflowValidationHelper.validateWorkflow(
                request.getWorkflowId(), userId, WorkflowStatusEnum.SCRIPT_GEN_DONE);
        if (!validateResponse.isSuccess()) {
            return SingleResponse.buildFailure(validateResponse.getErrCode(), validateResponse.getErrMessage());
        }

        // 2. 提交任务
        return storyboardTaskExecutor.submitTask(request.getWorkflowId(), request);
    }

    /**
     * 查询任务状态
     * @param taskId
     * @return
     */
    public SingleResponse<StoryboardWorkflowTaskStatusDTO> getStoryboardTaskStatus(Long taskId) {
        FicWorkflowTaskBO ficWorkflowTaskBO = ficWorkflowTaskRepository.findById(taskId);
        if (ficWorkflowTaskBO == null) {
            return SingleResponse.buildFailure("", "taskId不存在");
        }
        if (!TaskTypeEnum.STORYBOARD_TEXT_AND_IMG_GENERATION.name().equals(ficWorkflowTaskBO.getTaskType())) {
            return SingleResponse.buildFailure("", "不是分镜图生成任务");
        }

        List<FicAlgoTaskBO> ficAlgoTaskBOList = ficAlgoTaskRepository.findByWorkflowTaskId(ficWorkflowTaskBO.getId());
        StoryboardWorkflowTaskStatusDTO storyboardTaskStatusDTO = new StoryboardWorkflowTaskStatusDTO();

        if (TaskStatusEnum.FAILED.getCode().equals(ficWorkflowTaskBO.getStatus())) {
            return SingleResponse.buildFailure("", "分镜生成任务失败");
        } else if (TaskStatusEnum.COMPLETED.getCode().equals(ficWorkflowTaskBO.getStatus())) {
            List<Long> completedStoryboardIdList = StreamUtil.toStream(ficAlgoTaskBOList)
                    .filter(t -> TaskStatusEnum.COMPLETED.getCode().equals(t.getStatus()))
                    .map(FicAlgoTaskBO::getRelevantId).collect(Collectors.toList());

            storyboardTaskStatusDTO.setTaskId(taskId);
            storyboardTaskStatusDTO.setStatus(TaskStatusEnum.COMPLETED.name());
            storyboardTaskStatusDTO.setCompleteCnt(ficAlgoTaskBOList.size());
            storyboardTaskStatusDTO.setTotalCnt(ficAlgoTaskBOList.size());
            storyboardTaskStatusDTO.setCompletedStoryboardIds(completedStoryboardIdList);
        } else {
            List<Long> completedStoryboardIdList = StreamUtil.toStream(ficAlgoTaskBOList)
                    .filter(t -> TaskStatusEnum.COMPLETED.getCode().equals(t.getStatus()))
                    .map(FicAlgoTaskBO::getRelevantId).collect(Collectors.toList());
            storyboardTaskStatusDTO.setTaskId(taskId);
            storyboardTaskStatusDTO.setStatus(TaskStatusEnum.RUNNING.name());
            storyboardTaskStatusDTO.setCompleteCnt((int)completedStoryboardIdList.size());
            storyboardTaskStatusDTO.setTotalCnt(ficAlgoTaskBOList.size());
            storyboardTaskStatusDTO.setCompletedStoryboardIds(completedStoryboardIdList);
        }

        return SingleResponse.of(storyboardTaskStatusDTO);
    }

    /**
     * 将分镜BO和资源BO转换为DTO
     * @param ficStoryboardBO 分镜BO
     * @param ficResourceBO 资源BO
     * @return 分镜DTO
     */
    private StoryboardImgListItemDTO convertToDTO(FicStoryboardBO ficStoryboardBO, FicResourceBO ficResourceBO) {
        if (ficResourceBO == null) {
            return null;
        }

        StoryboardImgListItemDTO storyboardImgListItemDTO = new StoryboardImgListItemDTO();
        storyboardImgListItemDTO.setStoryboardId(ficStoryboardBO.getId());
        storyboardImgListItemDTO.setStoryboardResourceId(ficResourceBO.getId());
        storyboardImgListItemDTO.setImgUrl(ficResourceBO.getResourceUrl());
        storyboardImgListItemDTO.setOrderIndex(ficStoryboardBO.getOrderIndex());
        storyboardImgListItemDTO.setThumbnailUrl(ficResourceBO.getResourceUrl());
        return storyboardImgListItemDTO;
    }

    /**
     * 获取单张分镜信息
     * @param workflowId
     * @param storyboardId
     * @return
     */
    public SingleResponse<StoryboardImgListItemDTO> getSingleStoryboardImg(Long workflowId, Long storyboardId) {
        FicStoryboardBO ficStoryboardBO = ficStoryboardRepository.findById(storyboardId);
        if (ficStoryboardBO == null) {
            return SingleResponse.buildFailure("", "not exist storyboardId");
        }

        List<FicResourceBO> ficResourceBOList = ficResourceRepository.findByWorkflowIdAndResourceType(ficStoryboardBO.getWorkflowId(), ResourceTypeEnum.STORYBOARD_IMG);
        FicResourceBO ficResourceBO = StreamUtil.toStream(ficResourceBOList)
                .filter(t -> ficStoryboardBO.getId().equals(t.getRelevanceId()))
                .findFirst()
                .orElse(null);

        if (ficResourceBO == null) {
           return SingleResponse.buildFailure("", "分镜图未生成");
        }

        return SingleResponse.of(convertToDTO(ficStoryboardBO, ficResourceBO));
    }

    /**
     * 获取工作流下的所有分镜信息
     * @param workflowId 工作流ID
     * @return 分镜信息列表
     */
    public MultiResponse<StoryboardImgListItemDTO> getAllStoryboardImg(Long workflowId) {
        // 1. 获取工作流下的所有分镜
        List<FicStoryboardBO> ficStoryboardBOList = ficStoryboardRepository.findByWorkflowId(workflowId);
        if (ficStoryboardBOList.isEmpty()) {
            return MultiResponse.buildSuccess();
        }

        // 2. 获取所有分镜对应的资源信息
        List<FicResourceBO> ficResourceBOList = ficResourceRepository.findByWorkflowIdAndResourceType(workflowId, ResourceTypeEnum.STORYBOARD_IMG);

        // 3. 组装返回数据
        List<StoryboardImgListItemDTO> storyboardImgList = StreamUtil.toStream(ficStoryboardBOList)
                .map(ficStoryboardBO -> {
                    FicResourceBO ficResourceBO = StreamUtil.toStream(ficResourceBOList)
                            .filter(t -> ficStoryboardBO.getId().equals(t.getRelevanceId()))
                            .findFirst()
                            .orElse(null);
                    return convertToDTO(ficStoryboardBO, ficResourceBO);
                })
                .filter(t -> t != null)
                .collect(Collectors.toList());

        return MultiResponse.of(storyboardImgList);
    }

    public SingleResponse<Long> regenerateSingleStoryboard(Long userId, SingleStoryboardRegenRequest request) {
        // 1. 根据 storyboardId 查询分镜信息
        FicStoryboardBO ficStoryboardBO = ficStoryboardRepository.findById(request.getStoryboardId());
        if (ficStoryboardBO == null) {
            return SingleResponse.buildFailure("", "分镜不存在");
        }

        // 2. 校验工作流状态
        SingleResponse<?> validateResponse = workflowValidationHelper.validateWorkflow(
                ficStoryboardBO.getWorkflowId(), userId, WorkflowStatusEnum.STORYBOARD_IMG_GEN_DONE);
        if (!validateResponse.isSuccess()) {
            return SingleResponse.buildFailure(validateResponse.getErrCode(), validateResponse.getErrMessage());
        }

        // 3. 创建任务
        return singleStoryboardImgTaskExecutor.submitTask(ficStoryboardBO.getWorkflowId(), request);
    }
}
