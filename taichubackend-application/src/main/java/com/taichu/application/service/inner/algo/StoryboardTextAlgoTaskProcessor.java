package com.taichu.application.service.inner.algo;

import com.taichu.application.service.inner.algo.AlgoTaskBO;
import com.taichu.domain.algo.gateway.AlgoGateway;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 分镜文本生成任务处理器
 * 改为同步调用，直接处理结果
 */
@Component
@Slf4j
public class StoryboardTextAlgoTaskProcessor implements AlgoTaskProcessor {

    private final FicScriptRepository ficScriptRepository;
    private final FicStoryboardRepository ficStoryboardRepository;
    private final AlgoGateway algoGateway;
    private final FicWorkflowTaskRepository ficWorkflowTaskRepository;
    private final FicWorkflowRepository ficWorkflowRepository;

    @Autowired
    public StoryboardTextAlgoTaskProcessor(FicScriptRepository ficScriptRepository, 
                                         FicStoryboardRepository ficStoryboardRepository, 
                                         FicWorkflowTaskRepository ficWorkflowTaskRepository, 
                                         FicWorkflowRepository ficWorkflowRepository,
                                         AlgoGateway algoGateway) {
        this.ficScriptRepository = ficScriptRepository;
        this.ficStoryboardRepository = ficStoryboardRepository;
        this.ficWorkflowTaskRepository = ficWorkflowTaskRepository;
        this.ficWorkflowRepository = ficWorkflowRepository;
        this.algoGateway = algoGateway;
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
        
        List<AlgoTaskBO> resultList = new ArrayList<>();
        
        for (FicScriptBO ficScriptBO : ficScriptBOList) {
            log.info("[StoryboardTextAlgoTaskProcessor.generateTasks] 处理剧本片段, scriptId: {}", ficScriptBO.getId());
            
            // 同步调用算法服务
            StoryboardTextResult result = callAlgoService(workflowId, ficScriptBO);
            log.info("[StoryboardTextAlgoTaskProcessor.generateTasks] 算法服务响应: {}", result);
            
            if (result == null || CollectionUtils.isEmpty(result.getData())) {
                log.error("[StoryboardTextAlgoTaskProcessor.generateTasks] 算法服务返回结果为空, workflowId: {}, scriptId: {}", workflowId, ficScriptBO.getId());
                throw new RuntimeException("算法服务返回分镜文本结果为空, workflowId: " + workflowId + ", scriptId: " + ficScriptBO.getId());
            }
            
            // 直接处理结果，创建分镜记录
            processStoryboardTextResult(ficScriptBO, result);
            
            // 创建虚拟任务对象用于兼容接口
            AlgoTaskBO algoTaskBO = new AlgoTaskBO();
            algoTaskBO.setAlgoTaskId("SYNC_" + ficScriptBO.getId()); // 使用虚拟任务ID
            algoTaskBO.setRelevantId(ficScriptBO.getId());
            algoTaskBO.setRelevantIdType(RelevanceType.SCRIPT_ID);
            resultList.add(algoTaskBO);
            
            log.info("[StoryboardTextAlgoTaskProcessor.generateTasks] 处理完成, scriptId: {}, 生成分镜数量: {}", ficScriptBO.getId(), result.getData().size());
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
    private StoryboardTextResult callAlgoService(Long workflowId, FicScriptBO script) {
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

    /**
     * 处理分镜文本结果，直接创建分镜记录
     */
    private void processStoryboardTextResult(FicScriptBO script, StoryboardTextResult result) {
        if (result == null || CollectionUtils.isEmpty(result.getData())) {
            log.error("[StoryboardTextAlgoTaskProcessor.processStoryboardTextResult] 分镜文本结果为空, scriptId: {}", script.getId());
            throw new RuntimeException("分镜文本结果为空, scriptId: " + script.getId());
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
            log.info("[StoryboardTextAlgoTaskProcessor.processStoryboardTextResult] 创建分镜记录成功, scriptId: {}, storyboardId: {}", script.getId(), storyboard.getId());
            index++;
        }
        log.info("[StoryboardTextAlgoTaskProcessor.processStoryboardTextResult] 分镜文本处理完成, scriptId: {}, 分镜数量: {}", script.getId(), result.getData().size());
    }

    @Override
    public TaskStatusEnum checkSingleTaskStatus(FicAlgoTaskBO algoTask) {
        // 由于是同步处理，所有任务都已完成
        return TaskStatusEnum.COMPLETED;
    }

    @Override
    public void singleTaskSuccessPostProcess(FicAlgoTaskBO algoTask) {
        // 由于是同步处理，后置处理已在 generateTasks 中完成
        log.debug("[StoryboardTextAlgoTaskProcessor.singleTaskSuccessPostProcess] 同步处理已完成，无需额外后置处理, algoTaskId: {}", algoTask.getAlgoTaskId());
    }

    @Override
    public void postProcessAllComplete(FicWorkflowTaskBO workflowTask, List<FicAlgoTaskBO> algoTasks) {
        try {
            // 更新工作流任务状态为成功
            workflowTask.setStatus(TaskStatusEnum.COMPLETED.getCode());
            ficWorkflowTaskRepository.updateTaskStatus(workflowTask.getId(), TaskStatusEnum.COMPLETED);
            log.info("[StoryboardTextAlgoTaskProcessor.postProcessAllComplete] 所有分镜文本生成任务完成, workflowId: {}", workflowTask.getWorkflowId());
        } catch (Exception e) {
            log.error("[StoryboardTextAlgoTaskProcessor.postProcessAllComplete] 更新工作流任务状态失败, workflowId: {}", workflowTask.getWorkflowId(), e);
            workflowTask.setStatus(TaskStatusEnum.FAILED.getCode());
            ficWorkflowTaskRepository.updateTaskStatus(workflowTask.getId(), TaskStatusEnum.FAILED);
        }
    }

    @Override
    public void postProcessAnyFailed(FicWorkflowTaskBO workflowTask, List<FicAlgoTaskBO> algoTasks) {
        // 如果有任何任务失败，将工作流任务标记为失败
        workflowTask.setStatus(TaskStatusEnum.FAILED.getCode());
        ficWorkflowTaskRepository.updateTaskStatus(workflowTask.getId(), TaskStatusEnum.FAILED);
        log.error("[StoryboardTextAlgoTaskProcessor.postProcessAnyFailed] 分镜文本生成失败, workflowId: {}", workflowTask.getWorkflowId());
    }
}
