package com.taichu.application.service.inner.algo;

import com.taichu.domain.algo.gateway.AlgoGateway;
import com.taichu.domain.algo.gateway.FileGateway;
import com.taichu.domain.enums.*;
import com.taichu.infra.repo.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FullVideoRetryGenAlgoTaskProcessor extends FullVideoGenAlgoTaskProcessor {

    @Autowired
    public FullVideoRetryGenAlgoTaskProcessor(AlgoGateway algoGateway, FicWorkflowTaskRepository ficWorkflowTaskRepository, FicWorkflowRepository ficWorkflowRepository, FicStoryboardRepository ficStoryboardRepository, FileGateway fileGateway, FicResourceRepository ficResourceRepository) {
        super(algoGateway, ficWorkflowTaskRepository, ficWorkflowRepository, ficStoryboardRepository, fileGateway, ficResourceRepository);
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    @Override
    public AlgoTaskTypeEnum getAlgoTaskType() {
        return AlgoTaskTypeEnum.USER_RETRY_FULL_VIDEO_GENERATION;
    }

}
