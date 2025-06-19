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
        log.info("[SingleStoryboardVideoAlgoTaskProcessor.generateTasks] 开始生成单分镜视频任务, workflowTaskId: {}", workflowTask.getId());
        Long storyboardId = getStoryboardId(workflowTask);
        if (storyboardId == null) {
            log.error("[SingleStoryboardVideoAlgoTaskProcessor.generateTasks] 分镜ID为空, workflowTaskId: {}", workflowTask.getId());
            return Collections.emptyList();
        }
        Long workflowId = workflowTask.getWorkflowId();
        log.info("[SingleStoryboardVideoAlgoTaskProcessor.generateTasks] 处理分镜, storyboardId: {}, workflowId: {}", storyboardId, workflowId);
        FicStoryboardBO ficStoryboardBO = ficStoryboardRepository.findById(storyboardId);
        if (ficStoryboardBO == null) {
            log.error("[SingleStoryboardVideoAlgoTaskProcessor.generateTasks] 分镜不存在, storyboardId: {}", storyboardId);
            return List.of();
        }
        String operationName = "Call algorithm service for workflow: " + workflowId;
        AlgoResponse response = callAlgoServiceWithRetry(operationName, () -> callAlgoGenStoryboardVideo(workflowTask, ficStoryboardBO.getId()));
        log.info("[SingleStoryboardVideoAlgoTaskProcessor.generateTasks] 算法服务响应: {}", response);
        if (response == null) {
            log.error("[SingleStoryboardVideoAlgoTaskProcessor.generateTasks] Algorithm service failed to create storyboard_video task for workflow: {}, after {} retries", workflowId, getMaxRetry());
            return Collections.emptyList();
        }
        AlgoTaskBO algoTaskBO = new AlgoTaskBO();
        algoTaskBO.setAlgoTaskId(response.getTaskId());
        algoTaskBO.setRelevantId(storyboardId);
        algoTaskBO.setRelevantIdType(RelevanceType.STORYBOARD_ID);
        log.info("[SingleStoryboardVideoAlgoTaskProcessor.generateTasks] 生成的任务: {}", algoTaskBO);
        return Collections.singletonList(algoTaskBO);
    }

    static Long getStoryboardId(FicWorkflowTaskBO workflowTask) {
        String storyboardId = workflowTask.getParams().get(WorkflowTaskConstant.STORYBOARD_ID);
        log.debug("[SingleStoryboardVideoAlgoTaskProcessor.getStoryboardId] 从参数中获取分镜ID: {}", storyboardId);
        if (!StringUtils.isNumeric(storyboardId)) {
            log.warn("[SingleStoryboardVideoAlgoTaskProcessor.getStoryboardId] 分镜ID不是数字: {}", storyboardId);
            return null;
        }
        return Long.parseLong(storyboardId);
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

}
