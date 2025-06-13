package com.taichu.application.executor;

import com.taichu.application.service.inner.algo.AlgoTaskInnerService;
import com.taichu.domain.enums.AlgoTaskTypeEnum;
import com.taichu.domain.enums.TaskTypeEnum;
import com.taichu.domain.enums.WorkflowStatusEnum;
import com.taichu.domain.model.FicWorkflowTaskBO;
import com.taichu.infra.repo.FicWorkflowRepository;
import com.taichu.infra.repo.FicWorkflowTaskRepository;
import com.taichu.sdk.model.request.GenerateVideoRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class StoryboardVideoTaskExecutor extends AbstractTaskExecutor {
    private final AlgoTaskInnerService algoTaskInnerService;

    @Autowired
    public StoryboardVideoTaskExecutor(FicWorkflowRepository workflowRepository, FicWorkflowTaskRepository ficWorkflowTaskRepository, AlgoTaskInnerService algoTaskInnerService) {
        super(workflowRepository, ficWorkflowTaskRepository);
        this.algoTaskInnerService = algoTaskInnerService;
    }

    @Override
    protected Logger getLog() {
        return log;
    }

    @Override
    protected void startBackgroundProcessing(FicWorkflowTaskBO task) {
        try {
            algoTaskInnerService.runAlgoTask(task, AlgoTaskTypeEnum.STORYBOARD_VIDEO_GENERATION);
        } catch (Exception e) {
            // 发生异常，
            log.error("Background processing failed for workflow: " + task.getWorkflowId(), e);
        }

    }

    @Override
    protected WorkflowStatusEnum getNewWorkflowStatus() {
        return WorkflowStatusEnum.STORYBOARD_VIDEO_GEN_INIT;
    }

    @Override
    protected WorkflowStatusEnum getRollbackWorkflowStatus() {
        return WorkflowStatusEnum.STORYBOARD_IMG_GEN_DONE;
    }

    @Override
    protected TaskTypeEnum getWorkflowTaskType() {
        return TaskTypeEnum.STORYBOARD_VIDEO_GENERATION;
    }

    @Override
    protected Map<String, String> constructTaskParams(Long workflowId, Object request) {
        if (!(request instanceof GenerateVideoRequest)) {
            Map.of();
        }
        // TODO@chai看看有什么前端参数传过来，声音类型、视频风格
        return Map.of();
    }
}
