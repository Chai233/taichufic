package com.taichu.application.executor;

import com.taichu.application.service.inner.algo.v2.AlgoTaskInnerServiceV2;
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

import java.util.Map;
import java.util.Objects;

@Component
@Slf4j
public class SingleStoryboardImgTaskExecutor extends StoryboardTextAndImgTaskExecutor {
    private final AlgoTaskInnerServiceV2 algoTaskInnerServiceV2;

    public SingleStoryboardImgTaskExecutor(FicWorkflowRepository workflowRepository, FicWorkflowTaskRepository ficWorkflowTaskRepository, AlgoTaskInnerServiceV2 algoTaskInnerServiceV2) {
        super(workflowRepository, ficWorkflowTaskRepository, algoTaskInnerServiceV2);
        this.algoTaskInnerServiceV2 = algoTaskInnerServiceV2;
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
        GenerateStoryboardImgRequest storyboardImgRequest = (GenerateStoryboardImgRequest) request;
        Map<String, String> params = super.constructTaskParams(workflowId, request);
        params.put(WorkflowTaskConstant.STORYBOARD_ID, Objects.toString(storyboardImgRequest.getStoryboardId()));
        return params;
    }
} 