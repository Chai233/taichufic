package com.taichu.application.executor;

import com.alibaba.cola.dto.SingleResponse;
import com.taichu.domain.algo.gateway.AlgoGateway;
import com.taichu.domain.algo.model.AlgoResponse;
import com.taichu.domain.algo.model.request.ScriptTaskRequest;
import com.taichu.domain.enums.TaskStatusEnum;
import com.taichu.domain.enums.TaskTypeEnum;
import com.taichu.domain.enums.WorkflowStatusEnum;
import com.taichu.domain.model.FicAlgoTaskBO;
import com.taichu.domain.model.FicWorkflowTaskBO;
import com.taichu.infra.repo.FicAlgoTaskRepository;
import com.taichu.infra.repo.FicWorkflowRepository;
import com.taichu.infra.repo.FicWorkflowTaskRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class ScriptTaskExecutor {
    private static final long POLLING_INTERVAL = 5000 * 2; // 10秒轮询间隔

    @Autowired
    private FicWorkflowRepository workflowRepository;
    @Autowired
    private FicWorkflowTaskRepository ficWorkflowTaskRepository;

    @Autowired
    private FicAlgoTaskRepository ficAlgoTaskRepository;
    @Autowired
    private AlgoGateway algoGateway;
    @Autowired
    private StoryboardTextTaskExecutor storyboardExecutor;

    // 创建线程池
    private final ExecutorService executorService = new ThreadPoolExecutor(
        2,                      // 核心线程数
        4,                      // 最大线程数
        60L,                    // 空闲线程存活时间
        TimeUnit.SECONDS,       // 时间单位
        new LinkedBlockingQueue<>(100),  // 工作队列
        new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "script-task-" + threadNumber.getAndIncrement());
                t.setDaemon(true);  // 设置为守护线程
                return t;
            }
        },
        new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略
    );
    @Autowired
    private FicWorkflowRepository ficWorkflowRepository;

    public SingleResponse<Long> submitTask(Long workflowId) {
        try {
            // 1. 更新工作流状态
            workflowRepository.updateStatus(workflowId, WorkflowStatusEnum.SCRIPT_GEN.getCode());

            // 2. 创建任务记录
            FicWorkflowTaskBO ficWorkflowTaskBO = new FicWorkflowTaskBO();
            ficWorkflowTaskBO.setWorkflowId(workflowId);
            ficWorkflowTaskBO.setGmtCreate(System.currentTimeMillis());
            ficWorkflowTaskBO.setTaskType(TaskTypeEnum.SCRIPT_GENERATION.name());
            ficWorkflowTaskBO.setStatus(TaskStatusEnum.RUNNING.getCode());
            long workflowTaskId = (long)ficWorkflowTaskRepository.createFicWorkflowTask(ficWorkflowTaskBO);
            ficWorkflowTaskBO.setId(workflowTaskId);

            // 5. 提交后台任务到线程池
            executorService.submit(() -> startBackgroundProcessing(ficWorkflowTaskBO));

            return SingleResponse.of(workflowTaskId);
        } catch (Exception e) {
            log.error("Failed to submit script task for workflow: " + workflowId, e);
            // 如果任务记录都还没创建就失败了，只需要回滚工作流状态
            workflowRepository.updateStatus(workflowId, WorkflowStatusEnum.CLOSE.getCode());
            return SingleResponse.buildFailure("SCRIPT_001", "提交剧本生成任务失败: " + e.getMessage());
        }
    }

    /**
     * 回滚任务状态
     * @param task 任务对象
     */
    private void rollbackTask(FicWorkflowTaskBO task) {
        try {
            // 1. 更新任务状态为失败
            ficWorkflowTaskRepository.updateTaskStatus(task.getId(), TaskStatusEnum.FAILED);
        } catch (Exception e) {
            log.error("Failed to rollback task: " + task.getId(), e);
        }
    }

    protected void startBackgroundProcessing(FicWorkflowTaskBO task) {
        Long workflowId = task.getWorkflowId();
        Long workflowTaskId = task.getId();
        try {

            // 调用算法服务
            ScriptTaskRequest request = new ScriptTaskRequest();
            request.setWorkflowId(String.valueOf(workflowId));
            AlgoResponse response = algoGateway.createScriptTask(request);
            
            // 3.1 检查算法服务响应
            if (!response.isSuccess()) {
                log.error("Algorithm service failed to create script task for workflow: {}, error: {}", 
                    workflowId, response.getErrorMsg());
                rollbackTask(task);
                return;
            }
            
            // 3.2 检查任务ID
            if (response.getTaskId() == null || response.getTaskId().trim().isEmpty()) {
                log.error("Algorithm service returned empty task ID for workflow: {}", workflowId);
                rollbackTask(task);
                return ;
            }            

            // 4. 更新任务记录
            long algoTaskId = Long.parseLong(response.getTaskId());
            try {
                final FicAlgoTaskBO algoTask = new FicAlgoTaskBO();
                algoTask.setWorkflowTaskId(workflowTaskId);
                algoTask.setTaskType(TaskTypeEnum.SCRIPT_GENERATION.name());
                algoTask.setStatus(TaskStatusEnum.RUNNING.getCode());
                algoTask.setGmtCreate(System.currentTimeMillis());
                algoTask.setAlgoTaskId(algoTaskId);
                ficAlgoTaskRepository.save(algoTask);
            } catch (NumberFormatException e) {
                log.error("Invalid task ID format from algorithm service: {}", response.getTaskId());
                rollbackTask(task);
                return;
            }

 
            // 2.1 第一阶段成功，提交第二阶段任务
            SingleResponse<Long> storyboardResponse = storyboardExecutor.submitTask(task.getWorkflowId());
            if (!storyboardResponse.isSuccess()) {
                // 第二阶段任务提交失败，回滚第一阶段的状态
                rollbackTask(task);
                return;
            }

        } catch (Exception e) {
            // 发生异常，回滚所有状态
            log.error("Background processing failed for workflow: " + task.getWorkflowId(), e);
        }
    }




    // 在应用关闭时关闭线程池
    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
} 