package com.taichu.application.service.inner.algo;

import com.taichu.domain.algo.gateway.AlgoGateway;
import com.taichu.domain.algo.gateway.FileGateway;
import com.taichu.domain.algo.model.AlgoResponse;
import com.taichu.domain.enums.*;
import com.taichu.domain.model.*;
import com.taichu.infra.repo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class SingleStoryboardVideoAlgoTaskProcessor extends StoryboardVideoAlgoTaskProcessor {
    private final FicStoryboardRepository ficStoryboardRepository;

    protected SingleStoryboardVideoAlgoTaskProcessor(FicWorkflowTaskRepository ficWorkflowTaskRepository, FicWorkflowRepository ficWorkflowRepository, FicStoryboardRepository ficStoryboardRepository, AlgoGateway algoGateway, FicRoleRepository ficRoleRepository, FileGateway fileGateway, FicResourceRepository ficResourceRepository) {
        super(ficWorkflowTaskRepository, ficWorkflowRepository, ficStoryboardRepository, algoGateway, ficRoleRepository, fileGateway, ficResourceRepository);
        this.ficStoryboardRepository = ficStoryboardRepository;
    }


    @Override
    public AlgoTaskTypeEnum getAlgoTaskType() {
        return AlgoTaskTypeEnum.USER_RETRY_SINGLE_STORYBOARD_VIDEO_GENERATION;
    }

    @Override
    public List<AlgoTaskBO> generateTasks(FicWorkflowTaskBO workflowTask) {
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
        AlgoResponse response = callAlgoServiceWithRetry(operationName, () -> callAlgoGenStoryboardVideo(workflowTask, ficStoryboardBO.getId()));

        // 检查算法服务响应
        if (response == null) {
            log.error("Algorithm service failed to create storyboard_video task for workflow: {}, after {} retries",
                    workflowId, getMaxRetry());
            return Collections.emptyList();
        }

        // 添加到返回列表
        AlgoTaskBO algoTaskBO = new AlgoTaskBO();
        algoTaskBO.setAlgoTaskId(response.getTaskId());
        algoTaskBO.setRelevantId(storyboardId);
        algoTaskBO.setRelevantIdType(RelevanceType.STORYBOARD_ID);

        return Collections.singletonList(algoTaskBO);
    }

    static Long getStoryboardId(FicWorkflowTaskBO workflowTask) {
        String storyboardId = workflowTask.getParams().get(WorkflowTaskConstant.STORYBOARD_ID);
        if (!StringUtils.isNumeric(storyboardId)) {
            return null;
        }
        return Long.parseLong(storyboardId);
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

}
