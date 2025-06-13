package com.taichu.application.service.inner.algo;

import com.taichu.domain.algo.model.AlgoResponse;
import com.taichu.domain.enums.AlgoTaskTypeEnum;
import com.taichu.domain.enums.TaskStatusEnum;
import com.taichu.domain.model.FicAlgoTaskBO;
import com.taichu.domain.model.FicWorkflowTaskBO;

import java.util.List;

/**
 * TODO@chai
 */
public class StoryboardTextAlgoTaskProcessor implements AlgoTaskProcessor {

    @Override
    public AlgoTaskTypeEnum getAlgoTaskType() {
        return AlgoTaskTypeEnum.STORYBOARD_TEXT_GENERATION;
    }

    @Override
    public List<AlgoResponse> generateTasks(FicWorkflowTaskBO workflowTask) {
        return List.of();
    }

    @Override
    public TaskStatusEnum checkSingleTaskStatus(FicAlgoTaskBO algoTask) {
        return TaskStatusEnum.COMPLETED;
    }

    @Override
    public void singleTaskSuccessPostProcess(FicAlgoTaskBO algoTask) {

    }

    @Override
    public void postProcess(FicWorkflowTaskBO workflowTask, List<FicAlgoTaskBO> algoTasks) {

    }
}
