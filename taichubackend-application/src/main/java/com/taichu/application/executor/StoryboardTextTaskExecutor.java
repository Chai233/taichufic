package com.taichu.application.executor;

import com.alibaba.cola.dto.SingleResponse;
import com.taichu.domain.algo.gateway.AlgoGateway;
import com.taichu.domain.algo.model.AlgoResponse;
import com.taichu.domain.algo.model.request.StoryboardTextRequest;
import com.taichu.domain.enums.TaskStatusEnum;
import com.taichu.domain.enums.TaskTypeEnum;
import com.taichu.domain.enums.WorkflowStatusEnum;
import com.taichu.domain.model.FicTaskBO;
import com.taichu.infra.repo.FicTaskRepository;
import com.taichu.infra.repo.FicWorkflowRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StoryboardTextTaskExecutor {
    @Autowired
    private FicWorkflowRepository workflowRepository;
    @Autowired
    private FicTaskRepository taskRepository;
    @Autowired
    private AlgoGateway algoGateway;

    public SingleResponse<Long> submitTask(Long workflowId) {
        try {
            // 1. 更新工作流状态
            workflowRepository.updateStatus(workflowId, WorkflowStatusEnum.SCRIPT_GEN.getCode());

            // 2. 创建任务记录
            final FicTaskBO task = new FicTaskBO();
            task.setWorkflowId(workflowId);
            task.setTaskType(TaskTypeEnum.STORYBOARD_GENERATION.name());
            task.setStatus((byte) 1); // 执行中
            taskRepository.save(task);

            // 3. 调用算法服务
            StoryboardTextRequest request = new StoryboardTextRequest();
            request.setWorkflowId(String.valueOf(workflowId));
            AlgoResponse response = algoGateway.createStoryboardTextTask(request);
            
            // 3.1 检查算法服务响应
            if (!response.isSuccess()) {
                log.error("Algorithm service failed to create storyboard task for workflow: {}, error: {}", 
                    workflowId, response.getErrorMsg());
                rollbackTask(task, "ALGO_SERVICE_ERROR", response.getErrorMsg());
                return SingleResponse.buildFailure("STORYBOARD_002", "算法服务创建任务失败: " + response.getErrorMsg());
            }
            
            // 3.2 检查任务ID
            if (response.getTaskId() == null || response.getTaskId().trim().isEmpty()) {
                log.error("Algorithm service returned empty task ID for workflow: {}", workflowId);
                rollbackTask(task, "ALGO_TASK_ID_ERROR", "算法服务返回的任务ID为空");
                return SingleResponse.buildFailure("STORYBOARD_003", "算法服务返回的任务ID为空");
            }

            // 4. 更新任务记录
            try {
                task.setAlgoTaskId(Long.parseLong(response.getTaskId()));
                taskRepository.update(task);
            } catch (NumberFormatException e) {
                log.error("Invalid task ID format from algorithm service: {}", response.getTaskId());
                rollbackTask(task, "ALGO_TASK_ID_FORMAT_ERROR", "算法服务返回的任务ID格式无效");
                return SingleResponse.buildFailure("STORYBOARD_004", "算法服务返回的任务ID格式无效");
            }

            return SingleResponse.of(task.getId());
        } catch (Exception e) {
            log.error("Failed to submit storyboard task for workflow: " + workflowId, e);
            return SingleResponse.buildFailure("STORYBOARD_001", "提交分镜生成任务失败: " + e.getMessage());
        }
    }

    /**
     * 回滚任务状态
     * @param task 任务对象
     * @param errorCode 错误码
     * @param errorMsg 错误信息
     */
    private void rollbackTask(FicTaskBO task, String errorCode, String errorMsg) {
        try {
            // 1. 更新工作流状态为失败
            workflowRepository.updateStatus(task.getWorkflowId(), WorkflowStatusEnum.CLOSE.getCode());
            
            // 2. 更新任务状态为失败
            task.setStatus(TaskStatusEnum.FAILED.getCode());
            taskRepository.update(task);
            
            // 3. 记录错误日志
            log.error("Task rollback - workflowId: {}, taskId: {}, errorCode: {}, errorMsg: {}", 
                task.getWorkflowId(), task.getId(), errorCode, errorMsg);
        } catch (Exception e) {
            log.error("Failed to rollback task: " + task.getId(), e);
        }
    }
} 