package com.taichu.application.service.inner.algo;

import com.taichu.domain.algo.gateway.AlgoGateway;
import com.taichu.domain.algo.model.AlgoResponse;
import com.taichu.domain.algo.model.request.StoryboardTextRequest;
import com.taichu.domain.algo.model.response.StoryboardTextResult;
import com.taichu.domain.enums.AlgoTaskTypeEnum;
import com.taichu.domain.enums.CommonStatusEnum;
import com.taichu.domain.enums.RelevanceType;
import com.taichu.domain.enums.TaskStatusEnum;
import com.taichu.domain.model.FicAlgoTaskBO;
import com.taichu.domain.model.FicScriptBO;
import com.taichu.domain.model.FicStoryboardBO;
import com.taichu.domain.model.FicWorkflowTaskBO;
import com.taichu.infra.repo.FicScriptRepository;
import com.taichu.infra.repo.FicStoryboardRepository;
import com.taichu.infra.repo.FicWorkflowRepository;
import com.taichu.infra.repo.FicWorkflowTaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 分镜文本生成任务处理器
 */
@Component
@Slf4j
public class StoryboardTextAlgoTaskProcessor extends AbstractAlgoTaskProcessor {

    @Autowired
    private final FicScriptRepository ficScriptRepository;
    @Autowired
    private final FicStoryboardRepository ficStoryboardRepository;
    @Autowired
    private AlgoGateway algoGateway;

    @Autowired
    public StoryboardTextAlgoTaskProcessor(FicScriptRepository ficScriptRepository, FicStoryboardRepository ficStoryboardRepository, FicWorkflowTaskRepository ficWorkflowTaskRepository, FicWorkflowRepository ficWorkflowRepository) {
        super(ficWorkflowTaskRepository, ficWorkflowRepository);
        this.ficScriptRepository = ficScriptRepository;
        this.ficStoryboardRepository = ficStoryboardRepository;
    }

    @Override
    public AlgoTaskTypeEnum getAlgoTaskType() {
        return AlgoTaskTypeEnum.STORYBOARD_TEXT_GENERATION;
    }

    @Override
    public List<AlgoTaskBO> generateTasks(FicWorkflowTaskBO workflowTask) {
        Long workflowId = workflowTask.getWorkflowId();
        log.info("[StoryboardTextAlgoTaskProcessor.generateTasks] 开始生成分镜文本任务, workflowId: {}", workflowId);
        List<FicScriptBO> ficScriptBOList = ficScriptRepository.findByWorkflowId(workflowId);
        log.info("[StoryboardTextAlgoTaskProcessor.generateTasks] 查询到剧本片段: {}", ficScriptBOList);
        if (ficScriptBOList.isEmpty()) {
            log.warn("[StoryboardTextAlgoTaskProcessor.generateTasks] 剧本片段为空, workflowId: {}", workflowId);
            return List.of();
        }
        List<AlgoTaskBO> resultList = new ArrayList<>(ficScriptBOList.size());
        for (FicScriptBO ficScriptBO : ficScriptBOList) {
            log.info("[StoryboardTextAlgoTaskProcessor.generateTasks] 处理剧本片段, scriptId: {}", ficScriptBO.getId());
            String operationName = "Call algorithm service for workflow: " + workflowId;
            AlgoResponse response = callAlgoServiceWithRetry(operationName, () -> callAlgoService(workflowId, ficScriptBO));
            log.info("[StoryboardTextAlgoTaskProcessor.generateTasks] 算法服务响应: {}", response);
            if (response == null) {
                log.error("[StoryboardTextAlgoTaskProcessor.generateTasks] Algorithm service failed to create storyboard text task for workflow: {}, after {} retries", workflowId, getMaxRetry());
                return Collections.emptyList();
            }
            AlgoTaskBO algoTaskBO = new AlgoTaskBO();
            algoTaskBO.setAlgoTaskId(response.getTaskId());
            algoTaskBO.setRelevantId(ficScriptBO.getId());
            algoTaskBO.setRelevantIdType(RelevanceType.SCRIPT_ID);
            resultList.add(algoTaskBO);
            log.info("[StoryboardTextAlgoTaskProcessor.generateTasks] 添加任务: {}", algoTaskBO);
        }
        return resultList;
    }

    /**
     * 调用算法服务生成分镜文本
     * 
     * @param workflowId 工作流ID
     * @param script 剧本片段
     * @return 算法服务响应结果,如果剧本片段不存在则返回null
     */
    private AlgoResponse callAlgoService(Long workflowId, FicScriptBO script) {
        if (script == null) {
            log.error("剧本片段不存在, workflowId: {}", workflowId);
            return null;
        }

        // 构建请求参数并调用算法服务
        StoryboardTextRequest request = new StoryboardTextRequest();
        request.setWorkflow_id(String.valueOf(workflowId));
        request.setScript(script.getContent());
        return algoGateway.createStoryboardTextTask(request);
    }

    @Override
    public TaskStatusEnum checkSingleTaskStatus(FicAlgoTaskBO algoTask) {
        return super.checkSingleTaskStatus(algoTask);
    }

    @Override
    public void singleTaskSuccessPostProcess(FicAlgoTaskBO algoTask) {
        log.info("[StoryboardTextAlgoTaskProcessor.singleTaskSuccessPostProcess] 开始处理分镜文本, algoTaskId: {}", algoTask.getAlgoTaskId());
        StoryboardTextResult result = algoGateway.getStoryboardTextResult(Objects.toString(algoTask.getAlgoTaskId()));
        log.info("[StoryboardTextAlgoTaskProcessor.singleTaskSuccessPostProcess] 获取到分镜文本结果: {}", result);
        if (result == null || org.apache.commons.collections4.CollectionUtils.isEmpty(result.getData())) {
            log.error("[StoryboardTextAlgoTaskProcessor.singleTaskSuccessPostProcess] 获取分镜文本结果失败或结果为空, algoTaskId: {}", algoTask.getAlgoTaskId());
            return;
        }
        try {
            FicScriptBO script = ficScriptRepository.findById(algoTask.getRelevantId());
            if (script == null) {
                log.error("[StoryboardTextAlgoTaskProcessor.singleTaskSuccessPostProcess] 剧本片段不存在, scriptId: {}", algoTask.getRelevantId());
                return;
            }
            int index = 1;
            for (String storyboardContent : result.getData()) {
                long orderIndex = script.getOrderIndex() * 10000 + index;
                FicStoryboardBO storyboard = new FicStoryboardBO();
                storyboard.setWorkflowId(script.getWorkflowId());
                storyboard.setContent(storyboardContent);
                storyboard.setScriptId(script.getId());
                storyboard.setStatus(CommonStatusEnum.VALID.getValue());
                storyboard.setGmtCreate(System.currentTimeMillis());
                storyboard.setOrderIndex(orderIndex);
                ficStoryboardRepository.insert(storyboard);
                log.info("[StoryboardTextAlgoTaskProcessor.singleTaskSuccessPostProcess] 创建分镜记录成功, algoTaskId: {}, scriptId: {}, storyboardId: {}", algoTask.getAlgoTaskId(), script.getId(), storyboard.getId());
                index++;
            }
            log.info("[StoryboardTextAlgoTaskProcessor.singleTaskSuccessPostProcess] 分镜文本处理完成, algoTaskId: {}, scriptId: {}", algoTask.getAlgoTaskId(), script.getId());
        } catch (Exception e) {
            log.error("[StoryboardTextAlgoTaskProcessor.singleTaskSuccessPostProcess] 处理分镜文本失败, algoTaskId: {}", algoTask.getAlgoTaskId(), e);
        }
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    @Override
    protected AlgoGateway getAlgoGateway() {
        return algoGateway;
    }
}
