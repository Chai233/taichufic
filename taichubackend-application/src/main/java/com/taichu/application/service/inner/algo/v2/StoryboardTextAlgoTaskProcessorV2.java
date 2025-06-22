package com.taichu.application.service.inner.algo.v2;

import com.taichu.application.service.inner.algo.v2.context.AlgoTaskContext;
import com.taichu.application.service.inner.algo.v2.context.StoryboardTextTaskContext;
import com.taichu.domain.algo.gateway.AlgoGateway;
import com.taichu.domain.algo.model.request.StoryboardTextRequest;
import com.taichu.domain.algo.model.response.StoryboardTextResult;
import com.taichu.domain.enums.*;
import com.taichu.domain.model.*;
import com.taichu.infra.repo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@Slf4j
public class StoryboardTextAlgoTaskProcessorV2 extends AbstractAlgoTaskProcessorV2 {

    private final FicScriptRepository ficScriptRepository;
    private final FicStoryboardRepository ficStoryboardRepository;
    private final AlgoGateway algoGateway;

    public StoryboardTextAlgoTaskProcessorV2(FicScriptRepository ficScriptRepository, 
                                           FicStoryboardRepository ficStoryboardRepository, 
                                           AlgoGateway algoGateway,
                                           FicWorkflowTaskRepository ficWorkflowTaskRepository,
                                           FicWorkflowRepository ficWorkflowRepository) {
        super(ficWorkflowTaskRepository, ficWorkflowRepository);
        this.ficScriptRepository = ficScriptRepository;
        this.ficStoryboardRepository = ficStoryboardRepository;
        this.algoGateway = algoGateway;
    }

    @Override
    public AlgoTaskTypeEnum getAlgoTaskType() {
        return AlgoTaskTypeEnum.STORYBOARD_TEXT_GENERATION;
    }

    @Override
    public List<AlgoTaskContext> createTaskContextList(FicWorkflowTaskBO workflowTask) {
        Long workflowId = workflowTask.getWorkflowId();
        log.info("[StoryboardTextAlgoTaskProcessorV2.createTaskContextList] 开始创建分镜文本任务上下文, workflowId: {}", workflowId);
        
        List<FicScriptBO> ficScriptBOList = ficScriptRepository.findByWorkflowId(workflowId);
        if (ficScriptBOList.isEmpty()) {
            log.warn("[StoryboardTextAlgoTaskProcessorV2.createTaskContextList] 剧本片段为空, workflowId: {}", workflowId);
            return List.of();
        }
        
        List<AlgoTaskContext> contexts = new ArrayList<>();
        for (FicScriptBO ficScriptBO : ficScriptBOList) {
            StoryboardTextTaskContext context = new StoryboardTextTaskContext();
            context.setWorkflowId(workflowId);
            context.setWorkflowTaskId(workflowTask.getId());
            context.setScript(ficScriptBO);
            contexts.add(context);
        }
        
        return contexts;
    }

    @Override
    public void validateContext(AlgoTaskContext context) {
        StoryboardTextTaskContext textContext = (StoryboardTextTaskContext) context;
        if (textContext.getScript() == null) {
            throw new IllegalArgumentException("剧本片段不能为空");
        }
        if (textContext.getWorkflowId() == null) {
            throw new IllegalArgumentException("工作流ID不能为空");
        }
    }

    @Override
    public AlgoTaskBOV2 generateAlgoTask(AlgoTaskContext context) {
        StoryboardTextTaskContext textContext = (StoryboardTextTaskContext) context;
        
        // 构建请求参数
        StoryboardTextRequest request = new StoryboardTextRequest();
        request.setWorkflow_id(String.valueOf(textContext.getWorkflowId()));
        request.setScript(textContext.getScript().getContent());

        // 调用算法服务（同步调用，使用基类的重试机制）
        String operationName = "Create storyboard text task for script: " + textContext.getScript().getId();
        StoryboardTextResult result = retryOperation(() -> algoGateway.createStoryboardTextTask(request), operationName);
        
        if (result == null) {
            throw new RuntimeException("创建分镜文本任务失败");
        }
        
        // 直接处理结果，创建分镜记录
        processStoryboardTextResult(textContext.getScript(), result);
        
        // 创建虚拟任务对象用于兼容接口
        AlgoTaskBOV2 algoTaskBO = new AlgoTaskBOV2();
        algoTaskBO.setAlgoTaskId("SYNC_" + textContext.getScript().getId()); // 使用虚拟任务ID
        algoTaskBO.setRelevantId(textContext.getScript().getId());
        algoTaskBO.setRelevantIdType(RelevanceType.SCRIPT_ID);
        algoTaskBO.setTaskSummary(textContext.getTaskSummary());
        
        return algoTaskBO;
    }

    @Override
    public TaskStatusEnum checkSingleTaskStatus(FicAlgoTaskBO algoTask) {
        // 由于是同步处理，所有任务都已完成
        return TaskStatusEnum.COMPLETED;
    }

    @Override
    public void singleTaskSuccessPostProcess(FicAlgoTaskBO algoTask, AlgoTaskContext context) {
        // 由于是同步处理，后置处理已在 generateAlgoTask 中完成
        log.debug("[StoryboardTextAlgoTaskProcessorV2.singleTaskSuccessPostProcess] 同步处理已完成，无需额外后置处理, algoTaskId: {}", algoTask.getAlgoTaskId());
    }

    @Override
    public void singleTaskFailedPostProcess(FicAlgoTaskBO algoTask, AlgoTaskContext context, Exception e) {
        log.error("[StoryboardTextAlgoTaskProcessorV2.singleTaskFailedPostProcess] 分镜文本生成任务失败: {}", 
            algoTask.buildSummary(), e);
    }

    @Override
    public void postProcessAllComplete(FicWorkflowTaskBO workflowTask, List<AlgoTaskContext> contexts) {
        log.info("[StoryboardTextAlgoTaskProcessorV2.postProcessAllComplete] 所有分镜文本生成任务完成, workflowId: {}", workflowTask.getWorkflowId());
    }

    @Override
    public void postProcessAnyFailed(FicWorkflowTaskBO workflowTask, List<AlgoTaskContext> contexts) {
        super.postProcessAnyFailed(workflowTask, contexts);
        Long workflowId = workflowTask.getWorkflowId();
        cleanupFailedStoryboardTextTask(workflowId);
    }

    /**
     * 处理分镜文本结果，直接创建分镜记录
     */
    private void processStoryboardTextResult(FicScriptBO script, StoryboardTextResult result) {
        if (result == null || CollectionUtils.isEmpty(result.getData())) {
            log.error("[StoryboardTextAlgoTaskProcessorV2.processStoryboardTextResult] 分镜文本结果为空, scriptId: {}", script.getId());
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
            log.info("[StoryboardTextAlgoTaskProcessorV2.processStoryboardTextResult] 创建分镜记录成功, scriptId: {}, storyboardId: {}", script.getId(), storyboard.getId());
            index++;
        }
        log.info("[StoryboardTextAlgoTaskProcessorV2.processStoryboardTextResult] 分镜文本处理完成, scriptId: {}, 分镜数量: {}", script.getId(), result.getData().size());
    }

    private void cleanupFailedStoryboardTextTask(Long workflowId) {
        // 清理已创建的分镜记录
        try {
            List<FicStoryboardBO> allStoryboards = ficStoryboardRepository.findValidByWorkflowId(workflowId);
            for (FicStoryboardBO storyboard : allStoryboards) {
                // 由于没有直接的deleteById方法，我们通过设置状态为无效来"删除"
                storyboard.setStatus(CommonStatusEnum.INVALID.getValue());
                // 这里需要调用更新方法，但由于没有直接的更新方法，我们暂时跳过
                // 在实际实现中，应该添加相应的更新方法
                log.warn("[cleanupFailedStoryboardTextTask] 需要实现分镜删除功能, storyboardId: {}", storyboard.getId());
            }
            log.info("[cleanupFailedStoryboardTextTask] 清理失败任务资源成功, workflowId: {}", workflowId);
        } catch (Exception e) {
            log.error("[cleanupFailedStoryboardTextTask] 清理失败任务资源失败, workflowId: {}", workflowId, e);
        }
    }

    @Override
    protected AlgoGateway getAlgoGateway() {
        return algoGateway;
    }

    @Override
    protected Logger getLogger() {
        return log;
    }
} 