package com.taichu.application.executor;

import com.alibaba.cola.dto.SingleResponse;
import com.taichu.application.service.inner.AlgoTaskInnerService;
import com.taichu.domain.algo.gateway.AlgoGateway;
import com.taichu.domain.enums.AlgoTaskTypeEnum;
import com.taichu.domain.enums.TaskStatusEnum;
import com.taichu.domain.enums.TaskTypeEnum;
import com.taichu.domain.enums.WorkflowStatusEnum;
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
public class StoryboardTaskExecutor {
    private static final long POLLING_INTERVAL = 5000 * 2; // 10秒轮询间隔
    public static final int MAXIMUM_POOL_SIZE = 16;
    public static final int CORE_POOL_SIZE = 2;

    @Autowired
    private FicWorkflowRepository workflowRepository;
    @Autowired
    private FicWorkflowTaskRepository ficWorkflowTaskRepository;
    @Autowired
    private FicAlgoTaskRepository ficAlgoTaskRepository;
    @Autowired
    private AlgoGateway algoGateway;

    // 创建线程池
    private final ExecutorService executorService = new ThreadPoolExecutor(
            CORE_POOL_SIZE,                      // 核心线程数
            MAXIMUM_POOL_SIZE,                      // 最大线程数
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
    @Autowired
    private AlgoTaskInnerService algoTaskInnerService;

    public SingleResponse<Long> submitTask(Long workflowId) {
        try {
            // 1. 更新工作流状态
            workflowRepository.updateStatus(workflowId, WorkflowStatusEnum.STORYBOARD_IMG_GEN.getCode());

            // 2. 创建任务记录
            FicWorkflowTaskBO ficWorkflowTaskBO = new FicWorkflowTaskBO();
            ficWorkflowTaskBO.setWorkflowId(workflowId);
            ficWorkflowTaskBO.setGmtCreate(System.currentTimeMillis());
            ficWorkflowTaskBO.setTaskType(TaskTypeEnum.SCRIPT_GENERATION.name());
            ficWorkflowTaskBO.setStatus(TaskStatusEnum.RUNNING.getCode());
            long workflowTaskId = (long)ficWorkflowTaskRepository.createFicWorkflowTask(ficWorkflowTaskBO);
            ficWorkflowTaskBO.setId(workflowTaskId);

            // 3. 提交后台任务到线程池
            executorService.submit(() -> startBackgroundProcessing(ficWorkflowTaskBO));

            // 4. 返回任务id给前端
            return SingleResponse.of(workflowTaskId);
        } catch (Exception e) {
            log.error("Failed to submit script task for workflow: " + workflowId, e);
            // 如果任务记录都还没创建就失败了，只需要回滚工作流状态
            workflowRepository.updateStatus(workflowId, WorkflowStatusEnum.CLOSE.getCode());
            return SingleResponse.buildFailure("SCRIPT_001", "提交剧本生成任务失败: " + e.getMessage());
        }
    }


    protected void startBackgroundProcessing(FicWorkflowTaskBO task) {
        try {
            algoTaskInnerService.runAlgoTask(task, AlgoTaskTypeEnum.STORYBOARD_IMG_GENERATION);
            // TODO@chai检查 SCRIPT_GENERATION 任务状态
        } catch (Exception e) {
            // 发生异常，
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