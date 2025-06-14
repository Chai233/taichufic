package com.taichu.application.executor;

import com.taichu.application.service.inner.algo.AlgoTaskInnerService;
import com.taichu.domain.enums.AlgoTaskTypeEnum;
import com.taichu.domain.enums.TaskTypeEnum;
import com.taichu.domain.enums.WorkflowStatusEnum;
import com.taichu.domain.model.FicWorkflowTaskBO;
import com.taichu.infra.repo.FicWorkflowRepository;
import com.taichu.infra.repo.FicWorkflowTaskRepository;
import com.taichu.sdk.model.request.GenerateStoryboardImgRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class SingleStoryboardImgTaskExecutor extends AbstractTaskExecutor {
    private final AlgoTaskInnerService algoTaskInnerService;

    @Autowired
    public SingleStoryboardImgTaskExecutor(FicWorkflowRepository workflowRepository, FicWorkflowTaskRepository ficWorkflowTaskRepository, AlgoTaskInnerService algoTaskInnerService) {
        super(workflowRepository, ficWorkflowTaskRepository);
        this.algoTaskInnerService = algoTaskInnerService;
    }

    @Override
    protected Logger getLog() {
        return log;
    }

    @Override
    protected void doStartBackgroundProcessing(FicWorkflowTaskBO task) {
        try {
            algoTaskInnerService.runAlgoTask(task, AlgoTaskTypeEnum.USER_RETRY_SINGLE_STORYBOARD_IMG_GENERATION);
        } catch (Exception e) {
            // 发生异常，
            log.error("Background processing failed for workflow: " + task.getWorkflowId(), e);
        }
    }

    @Override
    protected WorkflowStatusEnum getInitWorkflowStatus() {
        return WorkflowStatusEnum.STORYBOARD_IMG_GEN_DONE;
    }

    @Override
    protected WorkflowStatusEnum getDoneWorkflowStatus() {
        return WorkflowStatusEnum.STORYBOARD_IMG_GEN_DONE;
    }

    @Override
    protected WorkflowStatusEnum getRollbackWorkflowStatus() {
        return WorkflowStatusEnum.STORYBOARD_IMG_GEN_DONE;
    }

    @Override
    protected TaskTypeEnum getWorkflowTaskType() {
        return TaskTypeEnum.USER_RETRY_SINGLE_STORYBOARD_IMG_GENERATION;
    }

    @Override
    protected Map<String, String> constructTaskParams(Long workflowId, Object request) {
        if (!(request instanceof GenerateStoryboardImgRequest)) {
            return Map.of();
        }
        GenerateStoryboardImgRequest regenRequest = (GenerateStoryboardImgRequest) request;
        return Map.of(
            "storyboardId", String.valueOf(regenRequest.getStoryboardId()),
            "paramWenbenyindaoqiangdu", String.valueOf(regenRequest.getScale()),
            "paramFenggeqiangdu", String.valueOf(regenRequest.getStyleScale()),
            "userPrompt", regenRequest.getUserPrompt()
        );
    }
} 