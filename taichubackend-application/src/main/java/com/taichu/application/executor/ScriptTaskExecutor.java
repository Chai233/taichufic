package com.taichu.application.executor;

import com.taichu.application.service.inner.algo.AlgoTaskInnerService;
import com.taichu.domain.algo.gateway.AlgoGateway;
import com.taichu.domain.enums.AlgoTaskTypeEnum;
import com.taichu.domain.enums.TaskTypeEnum;
import com.taichu.domain.enums.WorkflowStatusEnum;
import com.taichu.domain.model.FicWorkflowTaskBO;
import com.taichu.infra.repo.FicAlgoTaskRepository;
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
    protected void startBackgroundProcessing(FicWorkflowTaskBO task) {
        try {
            algoTaskInnerService.runAlgoTask(task, AlgoTaskTypeEnum.SCRIPT_GENERATION);
            // TODO@chai检查 SCRIPT_GENERATION 任务状态
            algoTaskInnerService.runAlgoTask(task, AlgoTaskTypeEnum.STORYBOARD_TEXT_GENERATION);
            // TODO@chai检查 STORYBOARD_TEXT_GENERATION 任务状态
        } catch (Exception e) {
            log.error("Background processing failed for workflow: " + task.getWorkflowId(), e);
        }
    }

    @Override
    protected WorkflowStatusEnum getNewWorkflowStatus() {
        return WorkflowStatusEnum.SCRIPT_GEN_INIT;
    }

    @Override
    protected WorkflowStatusEnum getRollbackWorkflowStatus() {
        return WorkflowStatusEnum.UPLOAD_FILE_DONE;
    }

    @Override
    protected TaskTypeEnum getWorkflowTaskType() {
        return TaskTypeEnum.SCRIPT_GENERATION;
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