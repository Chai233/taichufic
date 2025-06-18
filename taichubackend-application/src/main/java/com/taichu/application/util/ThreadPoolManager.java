package com.taichu.application.util;

import com.taichu.common.common.util.RequestContext;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ThreadPoolManager {
    private static final int MAXIMUM_POOL_SIZE = 16;
    private static final int CORE_POOL_SIZE = 2;
    private static volatile ThreadPoolManager INSTANCE;
    private static volatile ExecutorService executorService;

    private ThreadPoolManager() {
        log.info("Creating new ThreadPoolManager instance...");
        executorService = new ThreadPoolExecutor(
                CORE_POOL_SIZE,                      // 核心线程数
                MAXIMUM_POOL_SIZE,                   // 最大线程数
                60L,                                 // 空闲线程存活时间
                TimeUnit.SECONDS,                    // 时间单位
                new LinkedBlockingQueue<>(100),      // 工作队列
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
        log.info("ThreadPoolManager instance created successfully");
    }

    public static void init() {
        log.info("Initializing ThreadPoolManager...");
        if (INSTANCE == null) {
            synchronized (ThreadPoolManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ThreadPoolManager();
                    log.info("ThreadPoolManager initialized successfully");
                } else {
                    log.info("ThreadPoolManager already initialized");
                }
            }
        } else {
            log.info("ThreadPoolManager already initialized");
        }
    }

    public static ThreadPoolManager getInstance() {
        if (INSTANCE == null) {
            log.error("ThreadPoolManager not initialized. Please call init() first.");
            throw new IllegalStateException("ThreadPoolManager not initialized. Please call init() first.");
        }
        return INSTANCE;
    }

    private ExecutorService getExecutorService() {
        if (executorService == null) {
            log.error("ThreadPoolManager not initialized. Please call init() first.");
            throw new IllegalStateException("ThreadPoolManager not initialized. Please call init() first.");
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
        log.info("Shutting down ThreadPoolManager...");
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
                log.info("ThreadPoolManager shut down successfully");
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
                log.error("ThreadPoolManager shutdown interrupted", e);
            }
        } else {
            log.info("ThreadPoolManager was not initialized, nothing to shut down");
        }
    }
}