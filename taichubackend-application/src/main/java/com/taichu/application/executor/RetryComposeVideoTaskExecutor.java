package com.taichu.application.executor;

import com.taichu.application.service.inner.algo.AlgoTaskInnerService;
import com.taichu.domain.enums.AlgoTaskTypeEnum;
import com.taichu.domain.enums.TaskTypeEnum;
import com.taichu.domain.enums.WorkflowStatusEnum;
import com.taichu.domain.model.FicWorkflowTaskBO;
import com.taichu.infra.repo.FicWorkflowRepository;
import com.taichu.infra.repo.FicWorkflowTaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RetryComposeVideoTaskExecutor extends ComposeVideoTaskExecutor {
    private final AlgoTaskInnerService algoTaskInnerService;

    public RetryComposeVideoTaskExecutor(FicWorkflowRepository workflowRepository, FicWorkflowTaskRepository ficWorkflowTaskRepository, AlgoTaskInnerService algoTaskInnerService) {
        super(workflowRepository, ficWorkflowTaskRepository);
        this.algoTaskInnerService = algoTaskInnerService;
    }

    @Override
    protected void doStartBackgroundProcessing(FicWorkflowTaskBO task) {
        try {
            algoTaskInnerService.runAlgoTask(task, AlgoTaskTypeEnum.USER_RETRY_FULL_VIDEO_GENERATION);
        } catch (Exception e) {
            log.error("Background processing failed for workflow: " + task.getWorkflowId(), e);
        }
    }

    @Override
    protected WorkflowStatusEnum getInitWorkflowStatus() {
        return WorkflowStatusEnum.FULL_VIDEO_GEN_DONE;
    }

    @Override
    protected WorkflowStatusEnum getRollbackWorkflowStatus() {
        return WorkflowStatusEnum.FULL_VIDEO_GEN_DONE;
    }

    @Override
    protected TaskTypeEnum getWorkflowTaskType() {
        return TaskTypeEnum.USER_RETRY_FULL_VIDEO_GENERATION;
    }

    @Override
    public Logger getLog() {
        return log;
    }
}
