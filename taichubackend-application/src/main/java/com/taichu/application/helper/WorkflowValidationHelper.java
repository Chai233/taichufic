package com.taichu.application.helper;

import com.alibaba.cola.dto.SingleResponse;
import com.taichu.domain.enums.WorkflowStatusEnum;
import com.taichu.infra.persistance.model.FicTask;
import com.taichu.infra.persistance.model.FicWorkflow;
import com.taichu.infra.repo.FicTaskRepository;
import com.taichu.infra.repo.FicWorkflowRepository;
import com.taichu.infra.repo.query.SingleWorkflowQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 工作流校验助手类
 */
@Component
public class WorkflowValidationHelper {

    @Autowired
    private FicWorkflowRepository workflowRepository;

    @Autowired
    private FicTaskRepository taskRepository;

    /**
     * 校验工作流
     *
     * @param workflowId 工作流ID
     * @param userId 用户ID
     * @param expectedStatus 期望的状态
     * @return 校验结果
     */
    public SingleResponse<?> validateWorkflow(Long workflowId, Long userId, WorkflowStatusEnum expectedStatus) {
        // 1. 查询工作流信息（包含用户和状态校验）
        SingleWorkflowQuery query = SingleWorkflowQuery.builder()
                .workflowId(workflowId)
                .userId(userId)
                .status(expectedStatus.getCode())
                .build();
        
        Optional<FicWorkflow> workflowOpt = workflowRepository.findSingleWorkflow(query);
        
        // 2. 校验工作流是否存在
        if (workflowOpt.isEmpty()) {
            return SingleResponse.buildFailure("WORKFLOW_002", "工作流不存在");
        }
        
        // 3. 校验用户权限
        if (workflowOpt.get().getUserId().equals(userId)) {
            return SingleResponse.buildFailure("WORKFLOW_001", "用户不是该工作流的创建人");
        }
        
        // 4. 校验工作流状态
        if (!workflowOpt.get().getStatus().equals(expectedStatus.getCode())) {
            return SingleResponse.buildFailure("WORKFLOW_004", "工作流状态不是" + expectedStatus.getDescription());
        }
        
        // 5. 校验是否有任务正在执行中
        List<FicTask> tasks = taskRepository.findByWorkflowIdAndStatus(workflowId, (byte) 1); // 假设1表示执行中状态
        if (!tasks.isEmpty()) {
            return SingleResponse.buildFailure("WORKFLOW_003", "有任务正在执行中");
        }
        
        return SingleResponse.buildSuccess();
    }
} 