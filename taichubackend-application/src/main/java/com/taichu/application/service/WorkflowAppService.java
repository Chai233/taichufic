package com.taichu.application.service;

import com.alibaba.cola.dto.SingleResponse;
import com.taichu.application.annotation.EntranceLog;
import com.taichu.application.helper.WorkflowPageConvertHelper;
import com.taichu.application.service.user.util.AuthUtil;
import com.taichu.common.common.exception.AppServiceExceptionHandle;
import com.taichu.common.common.util.StreamUtil;
import com.taichu.domain.enums.TaskStatusEnum;
import com.taichu.domain.enums.TaskTypeEnum;
import com.taichu.domain.enums.WorkflowStatusEnum;
import com.taichu.domain.enums.ResourceTypeEnum;
import com.taichu.domain.model.FicWorkflowTaskBO;
import com.taichu.domain.model.FicResourceBO;
import com.taichu.infra.persistance.model.FicWorkflow;
import com.taichu.infra.persistance.model.FicWorkflowExample;
import com.taichu.infra.repo.FicWorkflowRepository;
import com.taichu.infra.repo.FicWorkflowMetaRepository;
import com.taichu.domain.model.FicWorkflowMetaBO;
import com.taichu.infra.repo.FicWorkflowTaskRepository;
import com.taichu.infra.repo.FicResourceRepository;
import com.taichu.sdk.constant.WorkflowTaskTypeEnum;
import com.taichu.sdk.model.WorkflowDTO;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 工作流应用服务
 */
@Slf4j
@Component
public class WorkflowAppService {

    @Autowired
    private FicWorkflowRepository workflowRepository;

    @Autowired
    private FicWorkflowMetaRepository ficWorkflowMetaRepository;

    @Autowired
    private WorkflowPageConvertHelper workflowPageConvertHelper;
    @Autowired
    private FicWorkflowTaskRepository ficWorkflowTaskRepository;
    
    @Autowired
    private FicResourceRepository ficResourceRepository;

    /**
     * 创建工作流
     * 1. 检查用户的所有工作流是否都是关闭状态或最后一个状态
     * 2. 将所有非关闭状态的工作流改为关闭状态
     * 3. 创建新的工作流
     *
     * @param userId 用户ID
     * @return 新创建的工作流ID
     */
    @EntranceLog(bizCode = "工作流创建")
    @AppServiceExceptionHandle(biz = "工作流创建")
    public SingleResponse<Long> createWorkflow(Long userId) {
        try {
            // 1. 查询用户的所有工作流
            FicWorkflowExample example = new FicWorkflowExample();
            example.createCriteria().andUserIdEqualTo(userId);
            List<FicWorkflow> workflows = workflowRepository.findByExample(example);

            // 2. 检查工作流状态
            for (FicWorkflow workflow : workflows) {
                Byte status = workflow.getStatus();
                // 如果不是关闭状态且不是最后一个状态（FULL_VIDEO_GEN_DONE），则返回错误
                if (WorkflowStatusEnum.CLOSE.getCode().equals(status)) {
                    // do nothing
                } else if (WorkflowStatusEnum.FULL_VIDEO_GEN_DONE.getCode().equals(status)) {
                    // do nothing
                } else {
                    return SingleResponse.buildFailure("WORKFLOW_008",
                        "存在未完成的工作流，请先完成或关闭现有工作流");
                }
            }

            // 3. 将所有非关闭状态的工作流改为关闭状态
            for (FicWorkflow workflow : workflows) {
                if (!WorkflowStatusEnum.CLOSE.getCode().equals(workflow.getStatus())) {
                    workflowRepository.updateStatus(workflow.getId(), WorkflowStatusEnum.CLOSE.getCode());
                }
            }

            // 4. 创建新的工作流
            FicWorkflow newWorkflow = new FicWorkflow();
            newWorkflow.setUserId(userId);
            newWorkflow.setGmtCreate(System.currentTimeMillis());
            newWorkflow.setStatus(WorkflowStatusEnum.INIT_WAIT_FOR_FILE.getCode());
            
            long workflowId = workflowRepository.insert(newWorkflow);

            // 新增：创建workflowMeta记录
            FicWorkflowMetaBO metaBO = new FicWorkflowMetaBO();
            metaBO.setWorkflowId(workflowId);
            ficWorkflowMetaRepository.insert(metaBO);

            return SingleResponse.of(workflowId);
        } catch (Exception e) {
            log.error("Failed to create workflow for user: " + userId, e);
            return SingleResponse.buildFailure("WORKFLOW_005", "创建工作流失败: " + e.getMessage());
        }
    }

    @EntranceLog(bizCode = "获取活跃工作流")
    @AppServiceExceptionHandle(biz = "获取活跃工作流")
    public SingleResponse<WorkflowDTO> getValidWorkflow(Long userId) {
        if (userId == null) {
            return SingleResponse.buildFailure("WORKFLOW_003", "用户ID不能为空");
        }
        FicWorkflowExample example = new FicWorkflowExample();
        example.createCriteria().andUserIdEqualTo(userId)
                .andStatusNotEqualTo(WorkflowStatusEnum.CLOSE.getCode());
        ;
        List<FicWorkflow> ficWorkflowList = workflowRepository.findByExample(example);
        if (CollectionUtils.isEmpty(ficWorkflowList)) {
            return SingleResponse.buildFailure("WORKFLOW_004", "没有生效中的的工作流");
        }
        FicWorkflow ficWorkflow = ficWorkflowList.get(0);

        FicWorkflowMetaBO ficWorkflowMetaBO = ficWorkflowMetaRepository.findByWorkflowId(ficWorkflow.getId());
        List<FicWorkflowTaskBO> tasks = ficWorkflowTaskRepository.findByWorkflowId(ficWorkflow.getId());
        FicWorkflowTaskBO runningTask = StreamUtil.toStream(tasks).filter(t -> TaskStatusEnum.RUNNING.getCode().equals(t.getStatus()))
                .findFirst().orElse(null);

        WorkflowDTO workflowDTO = new WorkflowDTO();
        workflowDTO.setWorkflowId(ficWorkflow.getId());

        WorkflowStatusEnum workflowStatusEnum = WorkflowStatusEnum.fromCode(ficWorkflow.getStatus());
        workflowDTO.setStatus(workflowStatusEnum.name());
        workflowDTO.setCurrentWorkflowPage(workflowPageConvertHelper.convertToWorkflowPage(workflowStatusEnum, runningTask).name());

        Optional.ofNullable(runningTask).ifPresent(t -> workflowDTO.setCurrentRunningTaskId(t.getId()));
        Optional.ofNullable(runningTask).ifPresent(t -> {
            TaskTypeEnum taskTypeEnum = TaskTypeEnum.valueOf(t.getTaskType());
            workflowDTO.setCurrentRunningTaskType(Objects.requireNonNull(convertTo(taskTypeEnum)).name());
        });

        if (ficWorkflowMetaBO != null) {
            workflowDTO.setTag(ficWorkflowMetaBO.getStyleType());
            workflowDTO.setScripUserPrompt(ficWorkflowMetaBO.getUserPrompt());
        }
        
        // 查询当前工作流的有效文件名
        List<FicResourceBO> novelFiles = ficResourceRepository.findValidByWorkflowIdAndResourceType(
                ficWorkflow.getId(), ResourceTypeEnum.NOVEL_FILE);
        List<String> uploadedFileNames = novelFiles.stream()
                .map(FicResourceBO::getOriginName)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        workflowDTO.setUploadedFileName(uploadedFileNames);
        
        return SingleResponse.of(workflowDTO);
    }

    static WorkflowTaskTypeEnum convertTo(TaskTypeEnum taskTypeEnum) {
        if (TaskTypeEnum.SCRIPT_AND_ROLE_GENERATION.equals(taskTypeEnum)) {
            return WorkflowTaskTypeEnum.SCRIPT;
        }

        if (TaskTypeEnum.STORYBOARD_TEXT_AND_IMG_GENERATION.equals(taskTypeEnum)
                || TaskTypeEnum.USER_RETRY_SINGLE_STORYBOARD_IMG_GENERATION.equals(taskTypeEnum)
        ) {
            return WorkflowTaskTypeEnum.STORYBOARD_IMG;
        }

        if (TaskTypeEnum.STORYBOARD_VIDEO_GENERATION.equals(taskTypeEnum)
                || TaskTypeEnum.USER_RETRY_SINGLE_STORYBOARD_VIDEO_GENERATION.equals(taskTypeEnum)
        ) {
            return WorkflowTaskTypeEnum.STORYBOARD_VIDEO;
        }

        if (TaskTypeEnum.FULL_VIDEO_GENERATION.equals(taskTypeEnum)
                || TaskTypeEnum.USER_RETRY_FULL_VIDEO_GENERATION.equals(taskTypeEnum)
        ) {
            return WorkflowTaskTypeEnum.MERGE_VIDEO;
        }

        return null;
    }

    /**
     * 关闭工作流
     * 1. 验证工作流权限（owner或admin）
     * 2. 将所有关联资源状态改为invalid
     * 3. 将工作流状态改为CLOSE
     *
     * @param workflowId 工作流ID
     * @param userId 用户ID
     * @return 关闭结果
     */
    @EntranceLog(bizCode = "关闭工作流")
    @AppServiceExceptionHandle(biz = "关闭工作流")
    public SingleResponse<Void> closeWorkflow(Long workflowId, Long userId) {
        try {
            // 1. 验证工作流权限（owner或admin）
            FicWorkflowExample example = new FicWorkflowExample();
            example.createCriteria().andIdEqualTo(workflowId);
            List<FicWorkflow> workflows = workflowRepository.findByExample(example);
            
            if (CollectionUtils.isEmpty(workflows)) {
                return SingleResponse.buildFailure("WORKFLOW_002", "工作流不存在");
            }
            
            FicWorkflow workflow = workflows.get(0);
            
            // 检查权限：只有工作流所有者或管理员可以关闭
            boolean isOwner = workflow.getUserId().equals(userId);
            boolean isAdmin = AuthUtil.isAdmin();
            
            if (!isOwner && !isAdmin) {
                return SingleResponse.buildFailure("WORKFLOW_001", "无权限操作此工作流");
            }
            
            if (isAdmin) {
                log.info("管理员权限验证通过, workflowId: {}, userId: {}", workflowId, userId);
            }
            
            // 2. 检查工作流状态，如果已经是关闭状态则直接返回成功
            if (WorkflowStatusEnum.CLOSE.getCode().equals(workflow.getStatus())) {
                log.info("工作流已经是关闭状态, workflowId: {}", workflowId);
                return SingleResponse.buildSuccess();
            }
            
            // 3. 将工作流状态改为CLOSE
            workflowRepository.updateStatus(workflowId, WorkflowStatusEnum.CLOSE.getCode());
            log.info("工作流已关闭, workflowId: {}", workflowId);

            // 4. 将所有关联资源状态改为invalid
            ficResourceRepository.offlineByWorkflowId(workflowId);
            log.info("已下线工作流的所有资源, workflowId: {}", workflowId);

            return SingleResponse.buildSuccess();
        } catch (Exception e) {
            log.error("关闭工作流失败, workflowId: {}, userId: {}", workflowId, userId, e);
            return SingleResponse.buildFailure("WORKFLOW_006", "关闭工作流失败: " + e.getMessage());
        }
    }
}