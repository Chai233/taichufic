package com.taichu.application.service.inner.algo.v2;

import com.taichu.application.service.inner.algo.v2.context.AlgoTaskContext;
import com.taichu.application.util.AlgoTaskThreadPoolManager;
import com.taichu.domain.enums.AlgoTaskTypeEnum;
import com.taichu.domain.enums.TaskStatusEnum;
import com.taichu.domain.model.FicAlgoTaskBO;
import com.taichu.domain.model.FicWorkflowTaskBO;
import com.taichu.infra.repo.FicAlgoTaskRepository;
import com.taichu.infra.repo.FicWorkflowTaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AlgoTaskInnerServiceV2 implements InitializingBean {
    
    public static final int WAIT_INTERVAL_MILLIS = 30000;
    private final FicAlgoTaskRepository ficAlgoTaskRepository;
    private final Map<AlgoTaskTypeEnum, AlgoTaskProcessorV2> taskProcessorMap = new ConcurrentHashMap<>();
    private final FicWorkflowTaskRepository ficWorkflowTaskRepository;

    public AlgoTaskInnerServiceV2(FicAlgoTaskRepository ficAlgoTaskRepository, 
                                 List<AlgoTaskProcessorV2> algoTaskProcessors, 
                                 FicWorkflowTaskRepository ficWorkflowTaskRepository) {
        this.ficAlgoTaskRepository = ficAlgoTaskRepository;
        this.ficWorkflowTaskRepository = ficWorkflowTaskRepository;
        
        // 注册所有处理器
        for (AlgoTaskProcessorV2 processor : algoTaskProcessors) {
            taskProcessorMap.put(processor.getAlgoTaskType(), processor);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // 初始化完成后的处理
        log.info("[AlgoTaskInnerServiceV2] V2版本算法任务服务初始化完成，注册了 {} 个处理器", taskProcessorMap.size());
    }

    /**
     * 运行算法任务
     */
    public void runAlgoTask(FicWorkflowTaskBO workflowTask, AlgoTaskTypeEnum algoTaskType) {
        Long workflowTaskId = workflowTask.getId();
        log.info("[AlgoTaskInnerServiceV2.runAlgoTask] 开始运行算法任务, workflowTaskId: {}, algoTaskType: {}", workflowTaskId, algoTaskType);
        
        try {
            // 1. 获取任务处理器
            AlgoTaskProcessorV2 processor = taskProcessorMap.get(algoTaskType);
            if (processor == null) {
                log.error("[AlgoTaskInnerServiceV2.runAlgoTask] 未找到任务类型[{}]的处理器", algoTaskType);
                return;
            }
            
            // 2. 创建任务上下文列表
            List<AlgoTaskContext> taskContexts = processor.createTaskContextList(workflowTask);
            log.info("[AlgoTaskInnerServiceV2.runAlgoTask] 创建了 {} 个任务上下文", taskContexts.size());
            
            if (taskContexts.isEmpty()) {
                log.warn("[AlgoTaskInnerServiceV2.runAlgoTask] 没有需要处理的任务");
                return;
            }
            
            // 3. 并发处理每个任务上下文
            List<AlgoTaskContext> completedContexts = new ArrayList<>();
            List<AlgoTaskContext> failedContexts = new ArrayList<>();
            
            // 创建Future列表来跟踪所有任务
            List<Future<TaskResult>> futures = new ArrayList<>();
            List<AlgoTaskContext> submittedContexts = new ArrayList<>();
            
            // 提交所有任务到线程池
            for (AlgoTaskContext context : taskContexts) {
                try {
                    // 验证上下文
                    processor.validateContext(context);
                    
                    // 提交任务到线程池
                    Future<TaskResult> future = AlgoTaskThreadPoolManager.getInstance().submit(() -> {
                        try {
                            boolean success = processSingleTaskWithRetry(processor, context, workflowTaskId);
                            return new TaskResult(context, success, null);
                        } catch (Exception e) {
                            log.error("[AlgoTaskInnerServiceV2.runAlgoTask] 处理任务上下文失败: {}", context.getTaskSummary(), e);
                            return new TaskResult(context, false, e);
                        }
                    });
                    futures.add(future);
                    submittedContexts.add(context);
                    
                } catch (Exception e) {
                    log.error("[AlgoTaskInnerServiceV2.runAlgoTask] 提交任务到线程池失败: {}", context.getTaskSummary(), e);
                    failedContexts.add(context);
                }
            }
            
            // 等待所有任务完成并收集结果
            for (int i = 0; i < futures.size(); i++) {
                Future<TaskResult> future = futures.get(i);
                AlgoTaskContext context = submittedContexts.get(i);
                
                try {
                    TaskResult result = future.get(30, TimeUnit.MINUTES); // 设置30分钟超时
                    if (result.isSuccess()) {
                        completedContexts.add(result.getContext());
                    } else {
                        failedContexts.add(result.getContext());
                        if (result.getException() != null) {
                            log.error("[AlgoTaskInnerServiceV2.runAlgoTask] 任务执行异常: {}", 
                                result.getContext().getTaskSummary(), result.getException());
                        }
                    }
                } catch (Exception e) {
                    log.error("[AlgoTaskInnerServiceV2.runAlgoTask] 等待任务完成时发生异常: {}", context.getTaskSummary(), e);
                    failedContexts.add(context);
                }
            }
            
            // 4. 后置处理
            if (failedContexts.isEmpty()) {
                log.info("[AlgoTaskInnerServiceV2.runAlgoTask] 所有任务完成，执行成功后置处理");
                processor.postProcessAllComplete(workflowTask, taskContexts);
            } else {
                log.error("[AlgoTaskInnerServiceV2.runAlgoTask] 存在失败任务，执行失败后置处理");
                processor.postProcessAnyFailed(workflowTask, taskContexts);
                throw new RuntimeException("部分算法任务失败");
            }
            
        } catch (Exception e) {
            log.error("[AlgoTaskInnerServiceV2.runAlgoTask] 运行算法任务失败, workflowTaskId: {}, algoTaskType: {}", workflowTaskId, algoTaskType, e);
            ficWorkflowTaskRepository.updateTaskStatus(workflowTaskId, TaskStatusEnum.FAILED);
            throw new RuntimeException("运行算法任务失败", e);
        }
    }
    
    /**
     * 任务结果包装类
     */
    private static class TaskResult {
        private final AlgoTaskContext context;
        private final boolean success;
        private final Exception exception;
        
        public TaskResult(AlgoTaskContext context, boolean success, Exception exception) {
            this.context = context;
            this.success = success;
            this.exception = exception;
        }
        
        public AlgoTaskContext getContext() {
            return context;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public Exception getException() {
            return exception;
        }
    }
    
    /**
     * 处理单个任务（支持重试）
     */
    private boolean processSingleTaskWithRetry(AlgoTaskProcessorV2 processor, AlgoTaskContext context, Long workflowTaskId) {
        FicAlgoTaskBO algoTask = null;
        
        while (context.canRetry()) {
            try {
                log.info("[AlgoTaskInnerServiceV2.processSingleTaskWithRetry] 开始处理任务: {}, 重试次数: {}", 
                    context.getTaskSummary(), context.getRetryCount());
                
                // 1. 生成算法任务
                AlgoTaskBOV2 algoTaskBO = processor.generateAlgoTask(context);
                if (algoTaskBO == null) {
                    throw new RuntimeException("生成算法任务失败");
                }
                
                // 2. 保存到数据库
                algoTask = saveAlgoTask(algoTaskBO, workflowTaskId, processor.getAlgoTaskType());
                log.info("[AlgoTaskInnerServiceV2.processSingleTaskWithRetry] 保存算法任务成功: {}", algoTask.getId());
                
                // 3. 轮询状态直到完成
                TaskStatusEnum finalStatus = pollTaskUntilComplete(processor, algoTask);
                
                // 4. 根据最终状态进行处理
                if (finalStatus == TaskStatusEnum.COMPLETED) {
                    // 任务成功，执行后置处理
                    processor.singleTaskSuccessPostProcess(algoTask, context);
                    algoTask.setStatus(TaskStatusEnum.COMPLETED.getCode());
                    ficAlgoTaskRepository.updateStatus(algoTask.getId(), TaskStatusEnum.COMPLETED);
                    log.info("[AlgoTaskInnerServiceV2.processSingleTaskWithRetry] 任务处理成功: {}", context.getTaskSummary());
                    return true;
                } else {
                    // 任务失败，执行失败后置处理
                    ficAlgoTaskRepository.updateStatus(algoTask.getId(), TaskStatusEnum.FAILED);
                    throw new RuntimeException("任务执行失败，最终状态: " + finalStatus + " algoTaskId: " + algoTask.getId());
                }
                
            } catch (Exception e) {
                log.error("[AlgoTaskInnerServiceV2.processSingleTaskWithRetry] 任务处理失败: {}, 重试次数: {}", 
                    context.getTaskSummary(), context.getRetryCount(), e);
                
                // 清理失败的任务
                if (algoTask != null) {
                    processor.singleTaskFailedPostProcess(algoTask, context, e);
                }
                
                context.incrementRetryCount();
                
                if (context.canRetry()) {
                    log.info("[AlgoTaskInnerServiceV2.processSingleTaskWithRetry] 准备重试任务: {}", context.getTaskSummary());
                    try {
                        Thread.sleep(10000); // 重试前等待10秒
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        log.error("[AlgoTaskInnerServiceV2.processSingleTaskWithRetry] 任务重试次数已达上限，最终失败: {}", context.getTaskSummary());
        return false;
    }
    
    /**
     * 轮询任务状态直到完成
     * 只负责状态查询，不处理业务逻辑
     */
    private TaskStatusEnum pollTaskUntilComplete(AlgoTaskProcessorV2 processor, FicAlgoTaskBO algoTask) {
        int pollCount = 0;
        while (pollCount < 40) { // 最多轮询40次，避免无限循环
            try {
                TaskStatusEnum status = processor.checkSingleTaskStatus(algoTask);
                
                if (status == TaskStatusEnum.COMPLETED || status == TaskStatusEnum.FAILED) {
                    log.info("[AlgoTaskInnerServiceV2.pollTaskUntilComplete] 任务状态已确定: {}, algoTaskId: {}", status, algoTask.getAlgoTaskId());
                    return status;
                } else {
                    // 继续轮询
                    Thread.sleep(WAIT_INTERVAL_MILLIS);
                    pollCount++;
                    log.debug("[AlgoTaskInnerServiceV2.pollTaskUntilComplete] 任务仍在运行，继续轮询: {}, 轮询次数: {}", algoTask.getAlgoTaskId(), pollCount);
                }
            } catch (Exception e) {
                log.error("[AlgoTaskInnerServiceV2.pollTaskUntilComplete] 轮询任务状态异常: {}", algoTask.getAlgoTaskId(), e);
                return TaskStatusEnum.FAILED;
            }
        }
        
        log.error("[AlgoTaskInnerServiceV2.pollTaskUntilComplete] 任务轮询超时: {}", algoTask.getAlgoTaskId());
        return TaskStatusEnum.FAILED;
    }
    
    /**
     * 保存算法任务到数据库
     */
    private FicAlgoTaskBO saveAlgoTask(AlgoTaskBOV2 algoTaskBO, Long workflowTaskId, AlgoTaskTypeEnum algoTaskType) {
        FicAlgoTaskBO ficAlgoTaskBO = new FicAlgoTaskBO();
        ficAlgoTaskBO.setGmtCreate(System.currentTimeMillis());
        ficAlgoTaskBO.setWorkflowTaskId(workflowTaskId);
        ficAlgoTaskBO.setStatus(TaskStatusEnum.RUNNING.getCode());
        ficAlgoTaskBO.setTaskType(algoTaskType.name());
        ficAlgoTaskBO.setAlgoTaskId(algoTaskBO.getAlgoTaskId());
        ficAlgoTaskBO.setRelevantId(algoTaskBO.getRelevantId());
        ficAlgoTaskBO.setRelevantIdType(algoTaskBO.getRelevantIdType().getValue());
        ficAlgoTaskBO.setTaskAbstract(algoTaskBO.getTaskSummary());
        
        return ficAlgoTaskRepository.save(ficAlgoTaskBO);
    }

} 