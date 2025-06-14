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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class ScriptTaskExecutor extends AbstractTaskExecutor {

    @Autowired
    private AlgoTaskInnerService algoTaskInnerService;

    public ScriptTaskExecutor(FicWorkflowRepository workflowRepository, FicWorkflowTaskRepository ficWorkflowTaskRepository) {
        super(workflowRepository, ficWorkflowTaskRepository);
    }

    @Override
    protected void doStartBackgroundProcessing(FicWorkflowTaskBO task) {
        algoTaskInnerService.runAlgoTask(task, AlgoTaskTypeEnum.SCRIPT_GENERATION);
        algoTaskInnerService.runAlgoTask(task, AlgoTaskTypeEnum.ROLE_IMG_GENERATION);
        algoTaskInnerService.runAlgoTask(task, AlgoTaskTypeEnum.STORYBOARD_TEXT_GENERATION);
    }

    @Override
    protected WorkflowStatusEnum getInitWorkflowStatus() {
        return WorkflowStatusEnum.SCRIPT_GEN_INIT;
    }

    @Override
    protected WorkflowStatusEnum getDoneWorkflowStatus() {
        return WorkflowStatusEnum.SCRIPT_GEN_DONE;
    }

    @Override
    protected WorkflowStatusEnum getRollbackWorkflowStatus() {
        return WorkflowStatusEnum.UPLOAD_FILE_DONE;
    }

    @Override
    protected TaskTypeEnum getWorkflowTaskType() {
        return TaskTypeEnum.SCRIPT_AND_ROLE_GENERATION;
    }

    @Override
    protected Map<String, String> constructTaskParams(Long workflowId, Object request) {
        return new HashMap<>();
    }

    @Override
    protected Logger getLog() {
        return log;
    }
}