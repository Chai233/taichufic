package com.taichu.application.util;

import com.taichu.common.common.util.RequestContext;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class AlgoTaskThreadPoolManager {
    private static final int MAXIMUM_POOL_SIZE = 2;
    private static final int CORE_POOL_SIZE = 2;
    private static volatile AlgoTaskThreadPoolManager INSTANCE;
    private static volatile ExecutorService executorService;

    private AlgoTaskThreadPoolManager() {
        log.info("Creating new AlgoTaskThreadPoolManager instance...");
        executorService = new ThreadPoolExecutor(
                CORE_POOL_SIZE,                      // 核心线程数
                MAXIMUM_POOL_SIZE,                   // 最大线程数
                60L,                                 // 空闲线程存活时间
                TimeUnit.SECONDS,                    // 时间单位
                new LinkedBlockingQueue<>(500),       // 工作队列
                new ThreadFactory() {
                    private final AtomicInteger threadNumber = new AtomicInteger(1);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "algo-task-" + threadNumber.getAndIncrement());
                        t.setDaemon(true);  // 设置为守护线程
                        return t;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略
        );
        log.info("AlgoTaskThreadPoolManager instance created successfully");
    }

    public static void init() {
        log.info("Initializing AlgoTaskThreadPoolManager...");
        if (INSTANCE == null) {
            synchronized (AlgoTaskThreadPoolManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AlgoTaskThreadPoolManager();
                    log.info("AlgoTaskThreadPoolManager initialized successfully");
                } else {
                    log.info("AlgoTaskThreadPoolManager already initialized");
                }
            }
        } else {
            log.info("AlgoTaskThreadPoolManager already initialized");
        }
    }

    public static AlgoTaskThreadPoolManager getInstance() {
        if (INSTANCE == null) {
            log.error("AlgoTaskThreadPoolManager not initialized. Please call init() first.");
            throw new IllegalStateException("AlgoTaskThreadPoolManager not initialized. Please call init() first.");
        }
        return INSTANCE;
    }

    private ExecutorService getExecutorService() {
        if (executorService == null) {
            log.error("AlgoTaskThreadPoolManager not initialized. Please call init() first.");
            throw new IllegalStateException("AlgoTaskThreadPoolManager not initialized. Please call init() first.");
        }
        return executorService;
    }

    public void execute(Runnable task) {
        String requestId = RequestContext.getRequestId();
        executorService.execute(() -> {
            try {
                if (requestId != null) {
                    RequestContext.setRequestId(requestId);
                }
                task.run();
            } finally {
                RequestContext.clear();
            }
        });
    }

    public <T> Future<T> submit(Callable<T> task) {
        String requestId = RequestContext.getRequestId();
        return executorService.submit(() -> {
            try {
                if (requestId != null) {
                    RequestContext.setRequestId(requestId);
                }
                return task.call();
            } finally {
                RequestContext.clear();
            }
        });
    }

    public static void shutdown() {
        log.info("Shutting down AlgoTaskThreadPoolManager...");
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
                log.info("AlgoTaskThreadPoolManager shut down successfully");
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
                log.error("AlgoTaskThreadPoolManager shutdown interrupted", e);
            }
        } else {
            log.info("AlgoTaskThreadPoolManager was not initialized, nothing to shut down");
        }
    }
} 