package com.taichu.application.executor;

import com.taichu.application.service.inner.algo.v2.AlgoTaskInnerServiceV2;
import com.taichu.domain.enums.AlgoTaskTypeEnum;
import com.taichu.domain.enums.TaskTypeEnum;
import com.taichu.domain.enums.WorkflowStatusEnum;
import com.taichu.domain.enums.WorkflowTaskConstant;
import com.taichu.domain.model.FicWorkflowMetaBO;
import com.taichu.domain.model.FicWorkflowTaskBO;
import com.taichu.infra.repo.FicWorkflowMetaRepository;
import com.taichu.infra.repo.FicWorkflowRepository;
import com.taichu.infra.repo.FicWorkflowTaskRepository;
import com.taichu.sdk.model.request.GenerateVideoRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class StoryboardVideoTaskExecutor extends AbstractTaskExecutor {
    private final AlgoTaskInnerServiceV2 algoTaskInnerServiceV2;
    private final FicWorkflowMetaRepository ficWorkflowMetaRepository;

    @Autowired
    public StoryboardVideoTaskExecutor(FicWorkflowRepository workflowRepository, FicWorkflowTaskRepository ficWorkflowTaskRepository, AlgoTaskInnerServiceV2 algoTaskInnerServiceV2, FicWorkflowMetaRepository ficWorkflowMetaRepository) {
        super(workflowRepository, ficWorkflowTaskRepository);
        this.algoTaskInnerServiceV2 = algoTaskInnerServiceV2;
        this.ficWorkflowMetaRepository = ficWorkflowMetaRepository;
    }

    @Override
    protected Logger getLog() {
        return log;
    }

    @Override
    protected void doStartBackgroundProcessing(FicWorkflowTaskBO task) throws Exception {
        algoTaskInnerServiceV2.runAlgoTask(task, AlgoTaskTypeEnum.STORYBOARD_VIDEO_GENERATION);
    }

    @Override
    public WorkflowStatusEnum getInitWorkflowStatus() {
        return WorkflowStatusEnum.STORYBOARD_VIDEO_GEN_INIT;
    }

    @Override
    public WorkflowStatusEnum getDoneWorkflowStatus() {
        return WorkflowStatusEnum.STORYBOARD_VIDEO_GEN_DONE;
    }

    @Override
    public WorkflowStatusEnum getRollbackWorkflowStatus() {
        return WorkflowStatusEnum.STORYBOARD_IMG_GEN_DONE;
    }

    @Override
    public TaskTypeEnum getWorkflowTaskType() {
        return TaskTypeEnum.STORYBOARD_VIDEO_GENERATION;
    }

    @Override
    protected Map<String, String> constructTaskParams(Long workflowId, Object request) {
        if (!(request instanceof GenerateVideoRequest)) {
            Map.of();
        }

        Map<String, String> map = new HashMap<>();
        FicWorkflowMetaBO ficWorkflowMetaBO = ficWorkflowMetaRepository.findByWorkflowId(workflowId);
        Optional.ofNullable(ficWorkflowMetaBO).map(FicWorkflowMetaBO::getStyleType)
                .ifPresent(t -> map.put(WorkflowTaskConstant.VIDEO_STYLE, t));
        return map;
    }
}
