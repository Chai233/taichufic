package com.taichu.application.service.inner.algo.v2;

import com.taichu.application.service.inner.algo.v2.context.AlgoTaskContext;
import com.taichu.domain.enums.AlgoTaskTypeEnum;
import com.taichu.domain.enums.TaskStatusEnum;
import com.taichu.domain.model.FicAlgoTaskBO;
import com.taichu.domain.model.FicWorkflowTaskBO;

import java.util.List;

/**
 * V2版本的算法任务处理器接口
 * 支持原子化处理和重试
 */
public interface AlgoTaskProcessorV2 {
    AlgoTaskTypeEnum getAlgoTaskType();
    
    // 创建任务上下文列表
    List<AlgoTaskContext> createTaskContextList(FicWorkflowTaskBO workflowTask);
    
    // 从上下文生成单个算法任务
    AlgoTaskBOV2 generateAlgoTask(AlgoTaskContext context);
    
    // 检查单个任务状态
    TaskStatusEnum checkSingleTaskStatus(FicAlgoTaskBO algoTask);
    
    // 单个任务成功后置处理
    void singleTaskSuccessPostProcess(FicAlgoTaskBO algoTask, AlgoTaskContext context) throws Exception;
    
    // 单个任务失败处理
    void singleTaskFailedPostProcess(FicAlgoTaskBO algoTask, AlgoTaskContext context, Exception e);
    
    // 所有任务完成后的处理
    void postProcessAllComplete(FicWorkflowTaskBO workflowTask, List<AlgoTaskContext> contexts);
    
    // 任何任务失败后的处理
    void postProcessAnyFailed(FicWorkflowTaskBO workflowTask, List<AlgoTaskContext> contexts);
    
    // 可选的上下文验证
    default void validateContext(AlgoTaskContext context) {
        // 默认实现为空，子类可以重写
    }
} 