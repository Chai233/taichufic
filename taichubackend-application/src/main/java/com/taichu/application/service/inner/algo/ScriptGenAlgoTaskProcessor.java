package com.taichu.application.service.inner.algo;

import com.taichu.domain.algo.gateway.AlgoGateway;
import com.taichu.domain.algo.model.AlgoResponse;
import com.taichu.domain.algo.model.request.ScriptTaskRequest;
import com.taichu.domain.enums.AlgoTaskTypeEnum;
import com.taichu.domain.enums.TaskStatusEnum;
import com.taichu.domain.model.FicAlgoTaskBO;
import com.taichu.domain.model.FicWorkflowTaskBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * TODO@chai
 */
@Component
@Slf4j
public class ScriptGenAlgoTaskProcessor implements AlgoTaskProcessor {

    @Autowired
    private AlgoGateway algoGateway;

    @Override
    public AlgoTaskTypeEnum getAlgoTaskType() {
        return AlgoTaskTypeEnum.SCRIPT_GENERATION;
    }

    @Override
    public List<AlgoResponse> generateTasks(FicWorkflowTaskBO workflowTask) {
        return null;
    }

    @Override
    public TaskStatusEnum checkSingleTaskStatus(FicAlgoTaskBO algoTask) {
        return null;
    }

    @Override
    public void singleTaskSuccessPostProcess(FicAlgoTaskBO algoTask) {

    }

    @Override
    public void postProcess(FicWorkflowTaskBO workflowTask, List<FicAlgoTaskBO> algoTasks) {

    }
}
