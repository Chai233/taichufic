package com.taichu.application.executor;

import com.taichu.application.service.inner.algo.v2.AlgoTaskInnerServiceV2;
import com.taichu.application.service.inner.algo.v2.SingleStoryboardImgAlgoTaskProcessorV2;
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
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
@Slf4j
public class SingleStoryboardImgTaskExecutor extends AbstractTaskExecutor {
    private final AlgoTaskInnerServiceV2 algoTaskInnerServiceV2;
    private final SingleStoryboardImgAlgoTaskProcessorV2 singleStoryboardImgAlgoTaskProcessorV2;

    public SingleStoryboardImgTaskExecutor(FicWorkflowRepository workflowRepository, FicWorkflowTaskRepository ficWorkflowTaskRepository, AlgoTaskInnerServiceV2 algoTaskInnerServiceV2, SingleStoryboardImgAlgoTaskProcessorV2 singleStoryboardImgAlgoTaskProcessorV2) {
        super(workflowRepository, ficWorkflowTaskRepository);
        this.algoTaskInnerServiceV2 = algoTaskInnerServiceV2;
        this.singleStoryboardImgAlgoTaskProcessorV2 = singleStoryboardImgAlgoTaskProcessorV2;
    }


    @Override
    protected Logger getLog() {
        return log;
    }

    @Override
    protected void doStartBackgroundProcessing(FicWorkflowTaskBO task) throws Exception {
        algoTaskInnerServiceV2.runAlgoTask(task, AlgoTaskTypeEnum.USER_RETRY_SINGLE_STORYBOARD_IMG_GENERATION);
    }

    @Override
    protected void doWhileBackgroundProcessingFail(FicWorkflowTaskBO task) {
        singleStoryboardImgAlgoTaskProcessorV2.postProcessAnyFailed(task, null);
    }

    @Override
    public WorkflowStatusEnum getInitWorkflowStatus() {
        return WorkflowStatusEnum.STORYBOARD_IMG_GEN_DONE;
    }

    @Override
    public WorkflowStatusEnum getDoneWorkflowStatus() {
        return WorkflowStatusEnum.STORYBOARD_IMG_GEN_DONE;
    }

    @Override
    public WorkflowStatusEnum getRollbackWorkflowStatus() {
        return WorkflowStatusEnum.STORYBOARD_IMG_GEN_DONE;
    }

    @Override
    public TaskTypeEnum getWorkflowTaskType() {
        return TaskTypeEnum.USER_RETRY_SINGLE_STORYBOARD_IMG_GENERATION;
    }

    @Override
    protected Map<String, String> constructTaskParams(Long workflowId, Object request) {
        if (!(request instanceof GenerateStoryboardImgRequest)) {
            return Map.of();
        }
        GenerateStoryboardImgRequest storyboardImgRequest = (GenerateStoryboardImgRequest) request;
        Long styleScale = storyboardImgRequest.getStyleScale();
        Long scale = storyboardImgRequest.getScale();
        String imageStyle = storyboardImgRequest.getImageStyle();

        Map<String, String> params = new HashMap<>();
        Optional.ofNullable(styleScale).ifPresent(s -> params.put(WorkflowTaskConstant.IMG_STYLE_SCALE, s.toString()));
        Optional.ofNullable(scale).ifPresent(s -> params.put(WorkflowTaskConstant.IMG_SCALE, s.toString()));
        Optional.ofNullable(imageStyle).ifPresent(s -> params.put(WorkflowTaskConstant.IMG_IMAGE_STYLE, s));

        params.put(WorkflowTaskConstant.STORYBOARD_ID, Objects.toString(storyboardImgRequest.getStoryboardId()));
        return params;
    }
} 