package com.taichu.application.executor;

import com.taichu.application.service.inner.algo.v2.AlgoTaskInnerServiceV2;
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
    private final AlgoTaskInnerServiceV2 algoTaskInnerServiceV2;

    public RetryComposeVideoTaskExecutor(FicWorkflowRepository workflowRepository, FicWorkflowTaskRepository ficWorkflowTaskRepository, AlgoTaskInnerServiceV2 algoTaskInnerServiceV2) {
        super(workflowRepository, ficWorkflowTaskRepository);
        this.algoTaskInnerServiceV2 = algoTaskInnerServiceV2;
    }

    @Override
    protected void doStartBackgroundProcessing(FicWorkflowTaskBO task) throws Exception {
        algoTaskInnerServiceV2.runAlgoTask(task, AlgoTaskTypeEnum.USER_RETRY_FULL_VIDEO_GENERATION);
    }

    @Override
    public WorkflowStatusEnum getInitWorkflowStatus() {
        return WorkflowStatusEnum.FULL_VIDEO_GEN_DONE;
    }

    @Override
    public WorkflowStatusEnum getRollbackWorkflowStatus() {
        return WorkflowStatusEnum.FULL_VIDEO_GEN_DONE;
    }

    @Override
    public TaskTypeEnum getWorkflowTaskType() {
        return TaskTypeEnum.USER_RETRY_FULL_VIDEO_GENERATION;
    }

    @Override
    public Logger getLog() {
        return log;
    }
}
