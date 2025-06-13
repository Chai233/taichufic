package com.taichu.application.service.inner.algo;

import com.taichu.domain.algo.gateway.AlgoGateway;
import com.taichu.domain.algo.model.AlgoResponse;
import com.taichu.domain.enums.TaskStatusEnum;
import com.taichu.domain.model.AlgoTaskStatus;
import com.taichu.domain.model.FicAlgoTaskBO;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.function.Supplier;

public abstract class AbstractAlgoTaskProcessor implements AlgoTaskProcessor {
    private static final int MAX_RETRY = 3;
    private static final int WAIT_INTERVAL = 5000;

    /**
     * 调用算法服务并处理重试逻辑
     *
     * @param operationName 操作名称（用于日志）
     * @param algoServiceCall 具体的算法服务调用逻辑
     * @return 算法服务响应，如果重试失败则返回null
     */
    protected AlgoResponse callAlgoServiceWithRetry(String operationName, Supplier<AlgoResponse> algoServiceCall) {
        return retryOperation(() -> {
            AlgoResponse response = algoServiceCall.get();
            
            if (!response.isSuccess()) {
                getLogger().error("{} failed, error: {}", operationName, response.getErrorMsg());
                return null;
            }

            if (response.getTaskId() == null || response.getTaskId().trim().isEmpty()) {
                getLogger().error("{} returned empty task ID", operationName);
                return null;
            }

            return response;
        }, operationName);
    }

    /**
     * 通用的重试方法
     *
     * @param operation 需要重试的操作
     * @param operationName 操作名称（用于日志）
     * @param <T> 操作返回类型
     * @return 操作结果，如果重试失败则返回null
     */
    protected <T> T retryOperation(Supplier<T> operation, String operationName) {
        int retryCount = 0;
        while (retryCount < getMaxRetry()) {
            try {
                T result = operation.get();
                if (result == null) {
                    getLogger().error("{} returned null, retry count: {}", operationName, retryCount + 1);
                    if (shouldRetry(retryCount, null)) {
                        retryCount++;
                        continue;
                    }
                    return null;
                }
                return result;
            } catch (Exception e) {
                getLogger().error("Unexpected error during {}: retry count: {}", operationName, retryCount + 1, e);
                if (shouldRetry(retryCount, null)) {
                    retryCount++;
                    continue;
                }
                return null;
            }
        }
        getLogger().error("Failed to complete {} after {} retries", operationName, getMaxRetry());
        return null;
    }

    abstract protected Logger getLogger();

    /**
     * 判断是否应该重试
     *
     * @param retryCount 当前重试次数
     * @param workflowId 工作流ID
     * @return 是否应该重试
     */
    protected boolean shouldRetry(int retryCount, Long workflowId) {
        if (retryCount < getMaxRetry() - 1) {
            try {
                Thread.sleep(getWaitInterval());
                return true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                getLogger().error("Retry interrupted for workflow: {}", workflowId);
                return false;
            }
        }
        return false;
    }

    protected abstract AlgoGateway getAlgoGateway();

    protected int getMaxRetry() {
        return AbstractAlgoTaskProcessor.MAX_RETRY;
    }

    protected int getWaitInterval() {
        return AbstractAlgoTaskProcessor.WAIT_INTERVAL;
    }

    @Override
    public TaskStatusEnum checkSingleTaskStatus(FicAlgoTaskBO algoTask) {
        AlgoTaskStatus taskStatus = getAlgoGateway().checkTaskStatus(Objects.toString(algoTask.getAlgoTaskId()));
        if (taskStatus.isCompleted()) {
            return TaskStatusEnum.COMPLETED;
        } else if (taskStatus.isRunning()) {
            return TaskStatusEnum.RUNNING;
        }

        return TaskStatusEnum.FAILED;
    }
}
