package com.taichu.application.service;

import com.alibaba.cola.dto.SingleResponse;
import com.taichu.application.annotation.EntranceLog;
import com.taichu.common.common.exception.AppServiceExceptionHandle;
import com.taichu.domain.enums.WorkflowStatusEnum;
import com.taichu.infra.persistance.model.FicWorkflow;
import com.taichu.infra.persistance.model.FicWorkflowExample;
import com.taichu.infra.repo.FicWorkflowRepository;
import com.taichu.infra.repo.FicWorkflowMetaRepository;
import com.taichu.domain.model.FicWorkflowMetaBO;
import com.taichu.sdk.model.WorkflowDTO;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

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
                if (!WorkflowStatusEnum.CLOSE.getCode().equals(status) 
                    && !WorkflowStatusEnum.FULL_VIDEO_GEN_DONE.getCode().equals(status)) {
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
            return SingleResponse.buildFailure("WORKFLOW_002", "创建工作流失败: " + e.getMessage());
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
        WorkflowDTO workflowDTO = new WorkflowDTO();
        workflowDTO.setWorkflowId(ficWorkflow.getId());
        workflowDTO.setStatus(WorkflowStatusEnum.fromCode(ficWorkflow.getStatus()).name());
        return SingleResponse.of(workflowDTO);
    }
}