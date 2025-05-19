package com.taichu.application.executor;

import com.alibaba.cola.dto.SingleResponse;
import com.taichu.domain.enums.TaskTypeEnum;
import com.taichu.domain.enums.WorkflowStatusEnum;
import com.taichu.domain.gateway.AlgoGateway;
import com.taichu.domain.model.AlgoResponse;
import com.taichu.domain.model.FicTaskBO;
import com.taichu.infra.repo.FicTaskRepository;
import com.taichu.infra.repo.FicWorkflowRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StoryboardTaskExecutor {
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
            FicTaskBO task = new FicTaskBO();
            task.setWorkflowId(workflowId);
            task.setTaskType(TaskTypeEnum.STORYBOARD_GENERATION.name());
            task.setStatus((byte) 1); // 执行中
            taskRepository.save(task);

            // 3. 调用算法服务
            AlgoResponse response = algoGateway.submitStoryboardTask(workflowId);
            task.setAlgoTaskId(response.getTaskId());
            taskRepository.update(task);

            return SingleResponse.of(task.getId());
        } catch (Exception e) {
            log.error("Failed to submit storyboard task for workflow: " + workflowId, e);
            return SingleResponse.buildFailure("STORYBOARD_001", "提交分镜生成任务失败");
        }
    }
} 