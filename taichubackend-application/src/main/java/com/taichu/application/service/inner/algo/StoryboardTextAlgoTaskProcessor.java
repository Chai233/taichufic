package com.taichu.application.service.inner.algo;

import com.taichu.domain.algo.gateway.AlgoGateway;
import com.taichu.domain.algo.model.AlgoResponse;
import com.taichu.domain.algo.model.request.StoryboardTextRequest;
import com.taichu.domain.algo.model.response.StoryboardTextResult;
import com.taichu.domain.enums.AlgoTaskTypeEnum;
import com.taichu.domain.enums.CommonStatusEnum;
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
    public List<AlgoResponse> generateTasks(FicWorkflowTaskBO workflowTask) {
        Long workflowId = workflowTask.getWorkflowId();

        // 1. 查询剧本片段
        List<FicScriptBO> ficScriptBOList = ficScriptRepository.findByWorkflowId(workflowId);
        if (ficScriptBOList.isEmpty()) {
            return List.of();
        }

        List<AlgoResponse> algoResponseList = new ArrayList<>(ficScriptBOList.size());
        for (FicScriptBO ficScriptBO : ficScriptBOList) {
            // 调用算法服务
            String operationName = "Call algorithm service for workflow: " + workflowId;
            AlgoResponse response = callAlgoServiceWithRetry(operationName, () -> callAlgoService(workflowId, ficScriptBO));

            // 检查算法服务响应
            if (response == null) {
                log.error("Algorithm service failed to create storyboard text task for workflow: {}, after {} retries",
                        workflowId, getMaxRetry());
                return Collections.emptyList();
            }

            // 添加到返回列表
            algoResponseList.add(response);
        }

        return algoResponseList;
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
        // 获取分镜文本生成结果
        StoryboardTextResult result = algoGateway.getStoryboardTextResult(Objects.toString(algoTask.getAlgoTaskId()));
        if (result == null || CollectionUtils.isEmpty(result.getData())) {
            log.error("获取分镜文本结果失败或结果为空, algoTaskId: {}", algoTask.getAlgoTaskId());
            return;
        }

        try {
            // 获取原始剧本片段
            FicScriptBO script = ficScriptRepository.findById(algoTask.getRelevantId());
            if (script == null) {
                log.error("剧本片段不存在, scriptId: {}", algoTask.getRelevantId());
                return;
            }

            // 为每个分镜文本创建分镜记录
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
                log.info("创建分镜记录成功, algoTaskId: {}, scriptId: {}, storyboardId: {}", 
                    algoTask.getAlgoTaskId(), script.getId(), storyboard.getId());
            }

            log.info("分镜文本处理完成, algoTaskId: {}, scriptId: {}", 
                algoTask.getAlgoTaskId(), script.getId());
        } catch (Exception e) {
            log.error("处理分镜文本失败, algoTaskId: {}", algoTask.getAlgoTaskId(), e);
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
