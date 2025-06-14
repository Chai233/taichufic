package com.taichu.application.service.inner.algo;

import com.taichu.domain.algo.gateway.AlgoGateway;
import com.taichu.domain.algo.gateway.FileGateway;
import com.taichu.domain.algo.model.AlgoResponse;
import com.taichu.domain.enums.AlgoTaskTypeEnum;
import com.taichu.domain.model.FicStoryboardBO;
import com.taichu.domain.model.FicWorkflowTaskBO;
import com.taichu.infra.repo.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

import static com.taichu.application.service.inner.algo.SingleStoryboardVideoAlgoTaskProcessor.getStoryboardId;

@Component
@Slf4j
public class SingleStoryboardImgAlgoTaskProcessor extends StoryboardImgAlgoTaskProcessor {

    private final FicStoryboardRepository ficStoryboardRepository;

    @Autowired
    public SingleStoryboardImgAlgoTaskProcessor(FicStoryboardRepository ficStoryboardRepository, AlgoGateway algoGateway, FicRoleRepository ficRoleRepository, FileGateway fileGateway, FicResourceRepository ficResourceRepository, FicWorkflowTaskRepository ficWorkflowTaskRepository, FicWorkflowRepository ficWorkflowRepository) {
        super(ficStoryboardRepository, algoGateway, ficRoleRepository, fileGateway, ficResourceRepository, ficWorkflowTaskRepository, ficWorkflowRepository);
        this.ficStoryboardRepository = ficStoryboardRepository;
    }

    @Override
    public AlgoTaskTypeEnum getAlgoTaskType() {
        return AlgoTaskTypeEnum.USER_RETRY_SINGLE_STORYBOARD_IMG_GENERATION;
    }

    @Override
    public List<AlgoResponse> generateTasks(FicWorkflowTaskBO workflowTask) {
        Long storyboardId = getStoryboardId(workflowTask);
        if (storyboardId == null) {
            log.error("分镜ID为空, workflowTaskId: {}", workflowTask.getId());
            return Collections.emptyList();
        }

        Long workflowId = workflowTask.getWorkflowId();

        // 1. 查询分镜
        FicStoryboardBO ficStoryboardBO = ficStoryboardRepository.findById(storyboardId);
        if (ficStoryboardBO == null) {
            return List.of();
        }

        // 调用算法服务
        String operationName = "Call algorithm service for workflow: " + workflowId;
        AlgoResponse response = callAlgoServiceWithRetry(operationName, () -> callAlgoServiceGenStoryboardImg(workflowTask, workflowId, ficStoryboardBO.getId()));

        // 检查算法服务响应
        if (response == null) {
            log.error("Algorithm service failed to create storyboard_video task for workflow: {}, after {} retries",
                    workflowId, getMaxRetry());
            return Collections.emptyList();
        }

        return Collections.singletonList(response);
    }

    @Override
    protected Logger getLogger() {
        return log;
    }
}
