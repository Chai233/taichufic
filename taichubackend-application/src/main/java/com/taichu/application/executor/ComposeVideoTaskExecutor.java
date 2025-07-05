package com.taichu.application.executor;

import com.taichu.application.service.inner.algo.v2.AlgoTaskInnerServiceV2;
import com.taichu.domain.enums.AlgoTaskTypeEnum;
import com.taichu.domain.enums.TaskTypeEnum;
import com.taichu.domain.enums.WorkflowStatusEnum;
import com.taichu.domain.enums.WorkflowTaskConstant;
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
import java.util.Optional;

@Slf4j
@Component
public class ComposeVideoTaskExecutor extends AbstractTaskExecutor {

    @Autowired
    private AlgoTaskInnerServiceV2 algoTaskInnerServiceV2;

    public ComposeVideoTaskExecutor(FicWorkflowRepository workflowRepository, FicWorkflowTaskRepository ficWorkflowTaskRepository) {
        super(workflowRepository, ficWorkflowTaskRepository);
    }

    @Override
    protected void doStartBackgroundProcessing(FicWorkflowTaskBO task) throws Exception {
        algoTaskInnerServiceV2.runAlgoTask(task, AlgoTaskTypeEnum.FULL_VIDEO_GENERATION);
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

        Map<String, String> params = new HashMap<>();
        ComposeVideoRequest composeVideoRequest = (ComposeVideoRequest) request;
        Optional.ofNullable(composeVideoRequest.getBgmType()).ifPresent(s -> params.put(WorkflowTaskConstant.VIDEO_BGM_TYPE, s));
        Optional.ofNullable(composeVideoRequest.getVoiceType()).ifPresent(s -> params.put(WorkflowTaskConstant.VIDEO_VOICE_TYPE, s));

        return params;
    }

    @Override
    protected Logger getLog() {
        return log;
    }
}