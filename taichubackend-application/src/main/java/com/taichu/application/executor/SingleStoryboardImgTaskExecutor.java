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
        log.info("[SingleStoryboardImgTaskExecutor.doStartBackgroundProcessing] 开始后台处理单分镜图片任务, workflowTaskId: {}, workflowId: {}", task.getId(), task.getWorkflowId());
        try {
            algoTaskInnerService.runAlgoTask(task, AlgoTaskTypeEnum.USER_RETRY_SINGLE_STORYBOARD_IMG_GENERATION);
            log.info("[SingleStoryboardImgTaskExecutor.doStartBackgroundProcessing] 单分镜图片任务处理完成, workflowTaskId: {}", task.getId());
        } catch (Exception e) {
            log.error("[SingleStoryboardImgTaskExecutor.doStartBackgroundProcessing] Background processing failed for workflow: " + task.getWorkflowId(), e);
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
        log.info("[SingleStoryboardImgTaskExecutor.constructTaskParams] 构建任务参数, workflowId: {}, request: {}", workflowId, request);
        if (!(request instanceof GenerateStoryboardImgRequest)) {
            log.warn("[SingleStoryboardImgTaskExecutor.constructTaskParams] 请求类型不匹配, expected: GenerateStoryboardImgRequest, actual: {}", request.getClass().getSimpleName());
            return Map.of();
        }
        GenerateStoryboardImgRequest regenRequest = (GenerateStoryboardImgRequest) request;
        Map<String, String> params = Map.of(
            "storyboardId", String.valueOf(regenRequest.getStoryboardId()),
            "paramWenbenyindaoqiangdu", String.valueOf(regenRequest.getScale()),
            "paramFenggeqiangdu", String.valueOf(regenRequest.getStyleScale()),
            "userPrompt", regenRequest.getUserPrompt()
        );
        log.info("[SingleStoryboardImgTaskExecutor.constructTaskParams] 构建的参数: {}", params);
        return params;
    }
} 