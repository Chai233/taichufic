package com.taichu.application.executor;

import com.taichu.application.service.inner.algo.v2.AlgoTaskInnerServiceV2;
import com.taichu.application.service.inner.algo.v2.RoleImgAlgoTaskProcessorV2;
import com.taichu.application.service.inner.algo.v2.ScriptGenAlgoTaskProcessorV2;
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
    ScriptGenAlgoTaskProcessorV2 scriptGenAlgoTaskProcessorV2;
    @Autowired
    RoleImgAlgoTaskProcessorV2 roleImgAlgoTaskProcessorV2;

    @Autowired
    private AlgoTaskInnerServiceV2 algoTaskInnerServiceV2;

    public ScriptTaskExecutor(FicWorkflowRepository workflowRepository, FicWorkflowTaskRepository ficWorkflowTaskRepository) {
        super(workflowRepository, ficWorkflowTaskRepository);
    }

    @Override
    protected void doStartBackgroundProcessing(FicWorkflowTaskBO task) throws Exception {
        algoTaskInnerServiceV2.runAlgoTask(task, AlgoTaskTypeEnum.SCRIPT_GENERATION);
        algoTaskInnerServiceV2.runAlgoTask(task, AlgoTaskTypeEnum.ROLE_IMG_GENERATION);
    }

    @Override
    protected void doWhileBackgroundProcessingFail(FicWorkflowTaskBO task) {
        scriptGenAlgoTaskProcessorV2.postProcessAnyFailed(task, null);
        roleImgAlgoTaskProcessorV2.postProcessAnyFailed(task, null);
    }

    @Override
    public WorkflowStatusEnum getInitWorkflowStatus() {
        return WorkflowStatusEnum.SCRIPT_GEN_INIT;
    }

    @Override
    public WorkflowStatusEnum getDoneWorkflowStatus() {
        return WorkflowStatusEnum.SCRIPT_GEN_DONE;
    }

    @Override
    public WorkflowStatusEnum getRollbackWorkflowStatus() {
        return WorkflowStatusEnum.UPLOAD_FILE_DONE;
    }

    @Override
    public TaskTypeEnum getWorkflowTaskType() {
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