package com.taichu.application.executor;

import com.taichu.application.service.inner.algo.AlgoTaskInnerService;
import com.taichu.domain.enums.AlgoTaskTypeEnum;
import com.taichu.domain.enums.TaskTypeEnum;
import com.taichu.domain.enums.WorkflowStatusEnum;
import com.taichu.domain.enums.WorkflowTaskConstant;
import com.taichu.domain.model.FicWorkflowTaskBO;
import com.taichu.infra.repo.FicWorkflowRepository;
import com.taichu.infra.repo.FicWorkflowTaskRepository;
import com.taichu.sdk.model.request.GenerateStoryboardImgRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class StoryboardTextAndImgTaskExecutor extends AbstractTaskExecutor {

    private final AlgoTaskInnerService algoTaskInnerService;

    @Autowired
    public StoryboardTextAndImgTaskExecutor(FicWorkflowRepository workflowRepository, FicWorkflowTaskRepository ficWorkflowTaskRepository, AlgoTaskInnerService algoTaskInnerService) {
        super(workflowRepository, ficWorkflowTaskRepository);
        this.algoTaskInnerService = algoTaskInnerService;
    }

    @Override
    protected Logger getLog() {
        return log;
    }

    @Override
    protected void doStartBackgroundProcessing(FicWorkflowTaskBO task) {
        algoTaskInnerService.runAlgoTask(task, AlgoTaskTypeEnum.STORYBOARD_TEXT_GENERATION);
        algoTaskInnerService.runAlgoTask(task, AlgoTaskTypeEnum.STORYBOARD_IMG_GENERATION);
    }

    @Override
    protected WorkflowStatusEnum getInitWorkflowStatus() {
        return WorkflowStatusEnum.STORYBOARD_IMG_GEN_INIT;
    }

    @Override
    protected WorkflowStatusEnum getDoneWorkflowStatus() {
        return WorkflowStatusEnum.STORYBOARD_IMG_GEN_DONE;
    }

    @Override
    protected WorkflowStatusEnum getRollbackWorkflowStatus() {
        return WorkflowStatusEnum.SCRIPT_GEN_DONE;
    }

    @Override
    protected TaskTypeEnum getWorkflowTaskType() {
        return TaskTypeEnum.STORYBOARD_TEXT_AND_IMG_GENERATION;
    }

    @Override
    protected Map<String, String> constructTaskParams(Long workflowId, Object request) {
        if (!(request instanceof GenerateStoryboardImgRequest)) {
            return Map.of();
        }
        GenerateStoryboardImgRequest storyboardImgRequest = (GenerateStoryboardImgRequest) request;
        Double styleScale = storyboardImgRequest.getStyleScale();
        Double scale = storyboardImgRequest.getScale();
        String imageStyle = storyboardImgRequest.getImageStyle();

        Map<String, String> params = new HashMap<>();
        Optional.ofNullable(styleScale).ifPresent(s -> params.put(WorkflowTaskConstant.IMG_STYLE_SCALE, s.toString()));
        Optional.ofNullable(scale).ifPresent(s -> params.put(WorkflowTaskConstant.IMG_SCALE, s.toString()));
        Optional.ofNullable(imageStyle).ifPresent(s -> params.put(WorkflowTaskConstant.IMG_IMAGE_STYLE, s));
        return params;

    }
}