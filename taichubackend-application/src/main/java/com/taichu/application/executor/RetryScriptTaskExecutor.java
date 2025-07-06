package com.taichu.application.executor;

import com.taichu.domain.enums.TaskTypeEnum;
import com.taichu.domain.enums.WorkflowStatusEnum;
import com.taichu.infra.repo.FicWorkflowRepository;
import com.taichu.infra.repo.FicWorkflowTaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RetryScriptTaskExecutor extends ScriptTaskExecutor {

    public RetryScriptTaskExecutor(FicWorkflowRepository workflowRepository, FicWorkflowTaskRepository ficWorkflowTaskRepository) {
        super(workflowRepository, ficWorkflowTaskRepository);
    }

    @Override
    public WorkflowStatusEnum getInitWorkflowStatus() {
        return WorkflowStatusEnum.SCRIPT_GEN_DONE;
    }

    @Override
    public WorkflowStatusEnum getDoneWorkflowStatus() {
        return WorkflowStatusEnum.SCRIPT_GEN_DONE;
    }

    @Override
    public WorkflowStatusEnum getRollbackWorkflowStatus() {
        return WorkflowStatusEnum.SCRIPT_GEN_DONE;
    }

    @Override
    public TaskTypeEnum getWorkflowTaskType() {
        return TaskTypeEnum.USER_RETRY_SCRIPT_AND_ROLE_GENERATION;
    }

    @Override
    protected Logger getLog() {
        return log;
    }
}