package com.taichu.application.executor;

import com.taichu.domain.enums.TaskTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务执行器工厂类
 * 根据任务类型获取对应的执行器
 */
@Slf4j
@Component
public class TaskExecutorFactory {

    private final Map<TaskTypeEnum, TaskExecutor> executorMap = new ConcurrentHashMap<>();

    @Autowired
    public TaskExecutorFactory(List<TaskExecutor> executors) {
        // 注册所有任务执行器
        for (TaskExecutor executor : executors) {
            TaskTypeEnum taskType = executor.getWorkflowTaskType();
            executorMap.put(taskType, executor);
            log.info("注册任务执行器: {} -> {}", taskType, executor.getClass().getSimpleName());
        }
        log.info("任务执行器工厂初始化完成，共注册 {} 个执行器", executorMap.size());
    }

    /**
     * 根据任务类型获取对应的执行器
     * @param taskType 任务类型
     * @return 任务执行器，如果不存在则返回null
     */
    public TaskExecutor getExecutor(TaskTypeEnum taskType) {
        TaskExecutor executor = executorMap.get(taskType);
        if (executor == null) {
            log.warn("未找到任务类型 {} 对应的执行器", taskType);
        }
        return executor;
    }

    /**
     * 根据任务类型字符串获取对应的执行器
     * @param taskTypeStr 任务类型字符串
     * @return 任务执行器，如果不存在则返回null
     */
    public TaskExecutor getExecutor(String taskTypeStr) {
        try {
            TaskTypeEnum taskType = TaskTypeEnum.valueOf(taskTypeStr);
            return getExecutor(taskType);
        } catch (IllegalArgumentException e) {
            log.warn("无效的任务类型字符串: {}", taskTypeStr);
            return null;
        }
    }

    /**
     * 检查是否存在指定任务类型的执行器
     * @param taskType 任务类型
     * @return 如果存在则返回true，否则返回false
     */
    public boolean hasExecutor(TaskTypeEnum taskType) {
        return executorMap.containsKey(taskType);
    }

    /**
     * 获取所有已注册的任务类型
     * @return 任务类型集合
     */
    public Set<TaskTypeEnum> getRegisteredTaskTypes() {
        return executorMap.keySet();
    }
} 