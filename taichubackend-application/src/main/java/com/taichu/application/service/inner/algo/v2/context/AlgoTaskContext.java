package com.taichu.application.service.inner.algo.v2.context;

import lombok.Getter;
import lombok.Setter;

/**
 * 算法任务上下文基类
 * 作为纯粹的数据容器，不包含业务逻辑
 */
@Setter
@Getter
public abstract class AlgoTaskContext {
    private static final int DEFAULT_MAX_RETRY_COUNT = 3;

    private Long workflowId;
    private Long workflowTaskId;
    private int retryCount = 0;
    private int maxRetryCount = DEFAULT_MAX_RETRY_COUNT;

    // 获取任务摘要信息（用于日志和调试）
    public abstract String getTaskSummary();
    
    public boolean canRetry() {
        return retryCount < maxRetryCount;
    }
    
    public void incrementRetryCount() {
        retryCount++;
    }

}