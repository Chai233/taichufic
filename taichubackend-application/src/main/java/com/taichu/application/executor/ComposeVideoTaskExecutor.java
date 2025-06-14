package com.taichu.application.executor;

import com.taichu.application.service.inner.algo.AlgoTaskInnerService;
import com.taichu.domain.enums.AlgoTaskTypeEnum;
import com.taichu.domain.enums.TaskTypeEnum;
import com.taichu.domain.enums.WorkflowStatusEnum;
import com.taichu.domain.model.FicWorkflowTaskBO;
import com.taichu.infra.repo.FicWorkflowRepository;
import com.taichu.infra.repo.FicWorkflowTaskRepository;
import com.taichu.sdk.model.request.ComposeVideoRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class ComposeVideoTaskExecutor extends AbstractTaskExecutor {

    @Autowired
    private AlgoTaskInnerService algoTaskInnerService;

    public ComposeVideoTaskExecutor(FicWorkflowRepository workflowRepository, FicWorkflowTaskRepository ficWorkflowTaskRepository) {
        super(workflowRepository, ficWorkflowTaskRepository);
    }

    @Override
    protected void doStartBackgroundProcessing(FicWorkflowTaskBO task) {
        try {
            algoTaskInnerService.runAlgoTask(task, AlgoTaskTypeEnum.FULL_VIDEO_GENERATION);
        } catch (Exception e) {
            log.error("Background processing failed for workflow: " + task.getWorkflowId(), e);
        }
    }

    @Override
    protected WorkflowStatusEnum getInitWorkflowStatus() {
        return WorkflowStatusEnum.FULL_VIDEO_GEN_INIT;
    }

    @Override
    protected WorkflowStatusEnum getDoneWorkflowStatus() {
        return WorkflowStatusEnum.FULL_VIDEO_GEN_DONE;
    }

    @Override
    protected WorkflowStatusEnum getRollbackWorkflowStatus() {
        return WorkflowStatusEnum.STORYBOARD_IMG_GEN_DONE;
    }

    @Override
    protected TaskTypeEnum getWorkflowTaskType() {
        return TaskTypeEnum.FULL_VIDEO_GENERATION;
    }

    @Override
    protected Map<String, String> constructTaskParams(Long workflowId, Object request) {
        if (!(request instanceof ComposeVideoRequest)) {
            return Map.of();
        }

        // TODO@chai 可能有特殊参数
        return new HashMap<>();
    }

    @Override
    protected Logger getLog() {
        return log;
    }
}