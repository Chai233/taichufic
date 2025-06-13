package com.taichu.application.service;

import com.alibaba.cola.dto.SingleResponse;
import com.taichu.application.executor.StoryboardTaskExecutor;
import com.taichu.application.helper.WorkflowValidationHelper;
import com.taichu.common.common.util.StreamUtil;
import com.taichu.domain.algo.gateway.FileGateway;
import com.taichu.domain.enums.*;
import com.taichu.domain.model.FicAlgoTaskBO;
import com.taichu.domain.model.FicResourceBO;
import com.taichu.domain.model.FicStoryboardBO;
import com.taichu.domain.model.FicWorkflowTaskBO;
import com.taichu.infra.persistance.mapper.FicResourceMapper;
import com.taichu.infra.repo.FicAlgoTaskRepository;
import com.taichu.infra.repo.FicResourceRepository;
import com.taichu.infra.repo.FicStoryboardRepository;
import com.taichu.infra.repo.FicWorkflowTaskRepository;
import com.taichu.sdk.model.StoryboardImgListItemDTO;
import com.taichu.sdk.model.StoryboardWorkflowTaskStatusDTO;
import com.taichu.sdk.model.request.GenerateStoryboardRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class StoryboardAppService {

    @Autowired
    private WorkflowValidationHelper workflowValidationHelper;

    @Autowired
    private StoryboardTaskExecutor storyboardTaskExecutor;
    @Autowired
    private FicWorkflowTaskRepository ficWorkflowTaskRepository;
    @Autowired
    private FicAlgoTaskRepository ficAlgoTaskRepository;
    @Autowired
    private FicStoryboardRepository ficStoryboardRepository;
    @Autowired
    private FicResourceMapper ficResourceMapper;
    @Autowired
    private FicResourceRepository ficResourceRepository;
    @Autowired
    private FileGateway fileDomainService;

    /**
     * 提交分镜图生成任务
     * @param request
     * @param userId
     * @return
     */
    public SingleResponse<Long> submitGenStoryboardTask(GenerateStoryboardRequest request, Long userId) {
        // 1. 校验工作流状态
        SingleResponse<?> validateResponse = workflowValidationHelper.validateWorkflow(
                request.getWorkflowId(), userId, WorkflowStatusEnum.SCRIPT_GEN);
        if (!validateResponse.isSuccess()) {
            return SingleResponse.buildFailure(validateResponse.getErrCode(), validateResponse.getErrMessage());
        }

        // 2. 提交任务
        return storyboardTaskExecutor.submitTask(request.getWorkflowId());
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
        if (!TaskTypeEnum.STORYBOARD_IMG_GENERATION.name().equals(ficWorkflowTaskBO.getTaskType())) {
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

        StoryboardImgListItemDTO storyboardImgListItemDTO = new StoryboardImgListItemDTO();
        storyboardImgListItemDTO.setStoryboardId(ficStoryboardBO.getId());
        storyboardImgListItemDTO.setStoryboardResourceId(ficResourceBO.getId());
        storyboardImgListItemDTO.setImgUrl(ficResourceBO.getResourceUrl());
        storyboardImgListItemDTO.setOrderIndex(ficStoryboardBO.getOrderIndex());
        storyboardImgListItemDTO.setThumbnailUrl(ficResourceBO.getResourceUrl());

        return SingleResponse.of(storyboardImgListItemDTO);

    }
}
