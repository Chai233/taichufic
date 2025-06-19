package com.taichu.application.service.inner.algo;

import com.taichu.domain.algo.gateway.AlgoGateway;
import com.taichu.domain.algo.gateway.FileGateway;
import com.taichu.domain.algo.model.AlgoResponse;
import com.taichu.domain.enums.AlgoTaskTypeEnum;
import com.taichu.domain.enums.RelevanceType;
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
        log.info("[SingleStoryboardImgAlgoTaskProcessor] 初始化单分镜图片处理器");
    }

    @Override
    public AlgoTaskTypeEnum getAlgoTaskType() {
        log.debug("[SingleStoryboardImgAlgoTaskProcessor.getAlgoTaskType] 获取任务类型: {}", AlgoTaskTypeEnum.USER_RETRY_SINGLE_STORYBOARD_IMG_GENERATION);
        return AlgoTaskTypeEnum.USER_RETRY_SINGLE_STORYBOARD_IMG_GENERATION;
    }

    @Override
    public List<AlgoTaskBO> generateTasks(FicWorkflowTaskBO workflowTask) {
        log.info("[SingleStoryboardImgAlgoTaskProcessor.generateTasks] 开始生成单分镜图片任务, workflowTaskId: {}", workflowTask.getId());
        Long storyboardId = getStoryboardId(workflowTask);
        if (storyboardId == null) {
            log.error("[SingleStoryboardImgAlgoTaskProcessor.generateTasks] 分镜ID为空, workflowTaskId: {}", workflowTask.getId());
            return Collections.emptyList();
        }
        Long workflowId = workflowTask.getWorkflowId();
        log.info("[SingleStoryboardImgAlgoTaskProcessor.generateTasks] 处理分镜, storyboardId: {}, workflowId: {}", storyboardId, workflowId);
        FicStoryboardBO ficStoryboardBO = ficStoryboardRepository.findById(storyboardId);
        if (ficStoryboardBO == null) {
            log.error("[SingleStoryboardImgAlgoTaskProcessor.generateTasks] 分镜不存在, storyboardId: {}", storyboardId);
            return List.of();
        }
        String operationName = "Call algorithm service for workflow: " + workflowId;
        AlgoResponse response = callAlgoServiceWithRetry(operationName, () -> callAlgoServiceGenStoryboardImg(workflowTask, workflowId, ficStoryboardBO.getId()));
        log.info("[SingleStoryboardImgAlgoTaskProcessor.generateTasks] 算法服务响应: {}", response);
        if (response == null) {
            log.error("[SingleStoryboardImgAlgoTaskProcessor.generateTasks] Algorithm service failed to create storyboard_video task for workflow: {}, after {} retries", workflowId, getMaxRetry());
            return Collections.emptyList();
        }
        AlgoTaskBO algoTaskBO = new AlgoTaskBO();
        algoTaskBO.setAlgoTaskId(response.getTaskId());
        algoTaskBO.setRelevantId(storyboardId);
        algoTaskBO.setRelevantIdType(RelevanceType.STORYBOARD_ID);
        log.info("[SingleStoryboardImgAlgoTaskProcessor.generateTasks] 生成的任务: {}", algoTaskBO);
        return Collections.singletonList(algoTaskBO);
    }

    @Override
    protected Logger getLogger() {
        return log;
    }
}
