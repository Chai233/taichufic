package com.taichu.application.service;

import com.alibaba.cola.dto.MultiResponse;
import com.alibaba.cola.dto.SingleResponse;
import com.taichu.application.annotation.EntranceLog;
import com.taichu.application.executor.StoryboardTextAndImgTaskExecutor;
import com.taichu.application.executor.SingleStoryboardImgTaskExecutor;
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
import com.taichu.sdk.model.StoryboardImgListItemDTO;
import com.taichu.sdk.model.StoryboardWorkflowTaskStatusDTO;
import com.taichu.sdk.model.request.GenerateStoryboardImgRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
    @Qualifier("storyboardTextAndImgTaskExecutor")
    private StoryboardTextAndImgTaskExecutor storyboardTaskExecutor;

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
    @EntranceLog(bizCode = "生成分镜")
    @AppServiceExceptionHandle(biz = "生成分镜")
    public SingleResponse<Long> submitGenStoryboardTask(GenerateStoryboardImgRequest request, Long userId) {
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
    @EntranceLog(bizCode = "查询分镜生成进度")
    @AppServiceExceptionHandle(biz = "查询分镜生成进度")
    public SingleResponse<StoryboardWorkflowTaskStatusDTO> getStoryboardTaskStatus(Long taskId) {
        FicWorkflowTaskBO ficWorkflowTaskBO = ficWorkflowTaskRepository.findById(taskId);
        if (ficWorkflowTaskBO == null) {
            return SingleResponse.buildFailure("", "taskId不存在");
        }
        
        // 检查任务类型
        boolean isStoryboardTextAndImgGeneration = TaskTypeEnum.STORYBOARD_TEXT_AND_IMG_GENERATION.name().equals(ficWorkflowTaskBO.getTaskType());
        boolean isUserRetrySingleStoryboardImgGeneration = TaskTypeEnum.USER_RETRY_SINGLE_STORYBOARD_IMG_GENERATION.name().equals(ficWorkflowTaskBO.getTaskType());
        
        if (!isStoryboardTextAndImgGeneration && !isUserRetrySingleStoryboardImgGeneration) {
            return SingleResponse.buildFailure("", "不是分镜图生成任务");
        }

        List<FicAlgoTaskBO> ficAlgoTaskBOList = ficAlgoTaskRepository.findByWorkflowTaskId(ficWorkflowTaskBO.getId());
        StoryboardWorkflowTaskStatusDTO storyboardTaskStatusDTO = new StoryboardWorkflowTaskStatusDTO();
        storyboardTaskStatusDTO.setTaskId(taskId);

        // 处理失败状态
        if (TaskStatusEnum.FAILED.getCode().equals(ficWorkflowTaskBO.getStatus())) {
            storyboardTaskStatusDTO.setStatus(TaskStatusEnum.FAILED.name());
            return SingleResponse.of(storyboardTaskStatusDTO);
        }

        // 处理完成状态
        if (TaskStatusEnum.COMPLETED.getCode().equals(ficWorkflowTaskBO.getStatus())) {
            List<Long> completedStoryboardIdList = getCompletedStoryboardIds(ficAlgoTaskBOList);
            storyboardTaskStatusDTO.setStatus(TaskStatusEnum.COMPLETED.name());
            storyboardTaskStatusDTO.setCompleteCnt(completedStoryboardIdList.size());
            storyboardTaskStatusDTO.setTotalCnt(completedStoryboardIdList.size());
            storyboardTaskStatusDTO.setCompletedStoryboardIds(completedStoryboardIdList);
            return SingleResponse.of(storyboardTaskStatusDTO);
        }

        // 处理运行中状态
        storyboardTaskStatusDTO.setStatus(TaskStatusEnum.RUNNING.name());
        
        if (isUserRetrySingleStoryboardImgGeneration) {
            // 用户重试单个分镜图片生成任务，只有图片生成部分
            setRunningStatusForImgOnly(storyboardTaskStatusDTO, ficAlgoTaskBOList);
        } else {
            // STORYBOARD_TEXT_AND_IMG_GENERATION 任务，需要检查文本生成任务
            setRunningStatusForTextAndImg(storyboardTaskStatusDTO, ficAlgoTaskBOList, ficWorkflowTaskBO.getWorkflowId());
        }

        return SingleResponse.of(storyboardTaskStatusDTO);
    }

    /**
     * 获取已完成的分镜ID列表
     */
    private List<Long> getCompletedStoryboardIds(List<FicAlgoTaskBO> ficAlgoTaskBOList) {
        return StreamUtil.toStream(ficAlgoTaskBOList)
                .filter(t -> AlgoTaskTypeEnum.STORYBOARD_IMG_GENERATION.name().equals(t.getTaskType())
                        && TaskStatusEnum.COMPLETED.getCode().equals(t.getStatus()))
                .map(FicAlgoTaskBO::getRelevantId)
                .collect(Collectors.toList());
    }

    /**
     * 设置仅图片生成任务的运行状态
     */
    private void setRunningStatusForImgOnly(StoryboardWorkflowTaskStatusDTO storyboardTaskStatusDTO, List<FicAlgoTaskBO> ficAlgoTaskBOList) {
        List<FicAlgoTaskBO> imgGenerationTasks = StreamUtil.toStream(ficAlgoTaskBOList)
                .filter(t -> AlgoTaskTypeEnum.STORYBOARD_IMG_GENERATION.name().equals(t.getTaskType()))
                .collect(Collectors.toList());

        List<Long> completedStoryboardIdList = StreamUtil.toStream(imgGenerationTasks)
                .filter(t -> TaskStatusEnum.COMPLETED.getCode().equals(t.getStatus()))
                .map(FicAlgoTaskBO::getRelevantId)
                .collect(Collectors.toList());

        storyboardTaskStatusDTO.setCompleteCnt(completedStoryboardIdList.size());
        storyboardTaskStatusDTO.setTotalCnt(1);
        storyboardTaskStatusDTO.setCompletedStoryboardIds(completedStoryboardIdList);
    }

    /**
     * 设置文本和图片生成任务的运行状态
     */
    private void setRunningStatusForTextAndImg(StoryboardWorkflowTaskStatusDTO storyboardTaskStatusDTO, List<FicAlgoTaskBO> ficAlgoTaskBOList, Long workflowId) {
        // 获取分镜总数
        List<FicStoryboardBO> allStoryboards = ficStoryboardRepository.findValidByWorkflowId(workflowId);
        int totalStoryboardCount = allStoryboards.size();

        // 获取已完成的分镜任务
        List<FicAlgoTaskBO> imgGenerationTasks = StreamUtil.toStream(ficAlgoTaskBOList)
                .filter(t -> AlgoTaskTypeEnum.STORYBOARD_IMG_GENERATION.name().equals(t.getTaskType()))
                .collect(Collectors.toList());
        List<Long> completedStoryboardIdList = StreamUtil.toStream(imgGenerationTasks)
                .filter(t -> TaskStatusEnum.COMPLETED.getCode().equals(t.getStatus()))
                .map(FicAlgoTaskBO::getRelevantId)
                .collect(Collectors.toList());

        storyboardTaskStatusDTO.setCompleteCnt(completedStoryboardIdList.size());
        storyboardTaskStatusDTO.setCompletedStoryboardIds(completedStoryboardIdList);
        storyboardTaskStatusDTO.setTotalCnt(totalStoryboardCount);
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
    @EntranceLog(bizCode = "获取单个分镜信息")
    @AppServiceExceptionHandle(biz = "获取单个分镜信息")
    public SingleResponse<StoryboardImgListItemDTO> getSingleStoryboardImg(Long workflowId, Long storyboardId) {
        FicStoryboardBO ficStoryboardBO = ficStoryboardRepository.findById(storyboardId);
        if (ficStoryboardBO == null) {
            return SingleResponse.buildFailure("", "not exist storyboardId");
        }

        List<FicResourceBO> ficResourceBOList = ficResourceRepository.findValidByWorkflowIdAndResourceType(ficStoryboardBO.getWorkflowId(), ResourceTypeEnum.STORYBOARD_IMG);
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
    @EntranceLog(bizCode = "获取全部分镜信息")
    @AppServiceExceptionHandle(biz = "获取全部分镜信息")
    public MultiResponse<StoryboardImgListItemDTO> getAllStoryboardImg(Long workflowId) {
        // 1. 获取工作流下的所有分镜
        List<FicStoryboardBO> ficStoryboardBOList = ficStoryboardRepository.findValidByWorkflowId(workflowId);
        if (ficStoryboardBOList.isEmpty()) {
            return MultiResponse.buildSuccess();
        }

        // 2. 获取所有分镜对应的资源信息
        List<FicResourceBO> ficResourceBOList = ficResourceRepository.findValidByWorkflowIdAndResourceType(workflowId, ResourceTypeEnum.STORYBOARD_IMG);

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

    @EntranceLog(bizCode = "单张分镜图重新生成")
    @AppServiceExceptionHandle(biz = "单张分镜图重新生成")
    public SingleResponse<Long> regenerateSingleStoryboard(Long userId, GenerateStoryboardImgRequest request) {
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
