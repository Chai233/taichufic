package com.taichu.application.executor;

import com.alibaba.cola.dto.SingleResponse;
import com.taichu.domain.algo.gateway.AlgoGateway;
import com.taichu.domain.algo.model.AlgoResponse;
import com.taichu.domain.algo.model.request.ScriptTaskRequest;
import com.taichu.domain.algo.model.response.ScriptResult;
import com.taichu.domain.enums.TaskStatusEnum;
import com.taichu.domain.enums.TaskTypeEnum;
import com.taichu.domain.enums.WorkflowStatusEnum;
import com.taichu.domain.model.FicTaskBO;
import com.taichu.domain.model.TaskStatus;
import com.taichu.infra.repo.FicTaskRepository;
import com.taichu.infra.repo.FicWorkflowRepository;
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
    private FicTaskRepository taskRepository;
    @Autowired
    private AlgoGateway algoGateway;
    @Autowired
    private StoryboardTaskExecutor storyboardExecutor;

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

    public SingleResponse<Long> submitTask(Long workflowId) {
        try {
            // 1. 更新工作流状态
            workflowRepository.updateStatus(workflowId, WorkflowStatusEnum.SCRIPT_GEN.getCode());

            // 2. 创建任务记录
            FicTaskBO task = new FicTaskBO();
            task.setWorkflowId(workflowId);
            task.setTaskType(TaskTypeEnum.SCRIPT_GENERATION.name());
            task.setStatus((byte) 1); // 执行中
            taskRepository.save(task);

            // 3. 调用算法服务
            ScriptTaskRequest request = new ScriptTaskRequest();
            request.setWorkflowId(String.valueOf(workflowId));
            AlgoResponse response = algoGateway.createScriptTask(request);
            task.setAlgoTaskId(Long.parseLong(response.getTaskId()));
            taskRepository.update(task);

            // 4. 提交后台任务到线程池
            executorService.submit(() -> startBackgroundProcessing(task));

            return SingleResponse.of(task.getId());
        } catch (Exception e) {
            log.error("Failed to submit script task for workflow: " + workflowId, e);
            return SingleResponse.buildFailure("SCRIPT_001", "提交剧本生成任务失败");
        }
    }

    protected void startBackgroundProcessing(FicTaskBO task) {
        try {
            // 1. 同步轮询第一阶段任务状态
            pollScriptTaskStatus(task);
            
            // 2. 处理第一阶段任务结果
            if (task.getStatus() == TaskStatusEnum.COMPLETED.getCode()) {
                // 2.1 第一阶段成功，提交第二阶段任务
                SingleResponse<Long> storyboardResponse = storyboardExecutor.submitTask(task.getWorkflowId());
                if (!storyboardResponse.isSuccess()) {
                    // 第二阶段任务提交失败，回滚第一阶段的状态
                    rollbackFirstStage(task);
                    return;
                }

                // 2.2 轮询第二阶段任务状态
                FicTaskBO storyboardTask = taskRepository.findById(storyboardResponse.getData());
                pollStoryboardTaskStatus(storyboardTask);

                // 2.3 处理第二阶段任务结果
                if (storyboardTask.getStatus() == TaskStatusEnum.COMPLETED.getCode()) {
                    // 两个阶段都成功，调用算法结果查询接口
                    ScriptResult result = algoGateway.getScriptResult(task.getAlgoTaskId().toString());
                    // 更新工作流状态为完成
                    // TODO@chai 更新工作流任务为完成，要建表！

                    // 更新工作流结果
                    // TODO@chai 更新工作流结果
                } else {
                    // 第二阶段失败，回滚所有状态
                    rollbackAllStages(task, storyboardTask);
                }
            } else {
                // 第一阶段失败，回滚状态
                rollbackFirstStage(task);
            }
        } catch (Exception e) {
            // 发生异常，回滚所有状态
            log.error("Background processing failed for workflow: " + task.getWorkflowId(), e);
        }
    }

    private void rollbackFirstStage(FicTaskBO task) {
        // 1. 更新工作流状态为失败
        workflowRepository.updateStatus(
            task.getWorkflowId(), 
            WorkflowStatusEnum.CLOSE.getCode()
        );
        // 2. 更新任务状态为失败
        task.setStatus(TaskStatusEnum.FAILED.getCode());
        taskRepository.update(task);
    }

    private void rollbackAllStages(FicTaskBO scriptTask, FicTaskBO storyboardTask) {
        // 1. 更新工作流状态为失败
        workflowRepository.updateStatus(
            scriptTask.getWorkflowId(), 
            WorkflowStatusEnum.CLOSE.getCode()
        );
        // 2. 更新第一阶段任务状态为失败
        scriptTask.setStatus(TaskStatusEnum.FAILED.getCode());
        taskRepository.update(scriptTask);
        // 3. 如果存在第二阶段任务，也更新为失败
        if (storyboardTask != null) {
            storyboardTask.setStatus(TaskStatusEnum.FAILED.getCode());
            taskRepository.update(storyboardTask);
        }
    }

    protected void pollScriptTaskStatus(FicTaskBO task) {
        while (true) {
            try {
                // 1. 检查算法任务状态
                TaskStatus status = algoGateway.checkTaskStatus(task.getAlgoTaskId().toString());
                
                // 2. 更新任务状态
                task.setStatus(status.getCode());
                taskRepository.update(task);
                
                // 3. 如果任务完成或失败，退出轮询
                if (status.isCompleted() || status.isFailed()) {
                    break;
                }
                
                Thread.sleep(POLLING_INTERVAL);
            } catch (Exception e) {
                log.error("Error polling script task status: " + task.getId(), e);
                break;
            }
        }
    }

    protected void pollStoryboardTaskStatus(FicTaskBO task) {
        while (true) {
            try {
                // 1. 检查算法任务状态
                TaskStatus status = algoGateway.checkTaskStatus(task.getAlgoTaskId().toString());
                
                // 2. 更新任务状态
                task.setStatus(status.getCode());
                taskRepository.update(task);
                
                // 3. 如果任务完成或失败，退出轮询
                if (status.isCompleted() || status.isFailed()) {
                    break;
                }
                
                Thread.sleep(POLLING_INTERVAL);
            } catch (Exception e) {
                log.error("Error polling storyboard task status: " + task.getId(), e);
                break;
            }
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