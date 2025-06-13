package com.taichu.application.util;

import javax.annotation.PreDestroy;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPoolManager {
    private static final ThreadPoolManager INSTANCE = new ThreadPoolManager();
    public static final int MAXIMUM_POOL_SIZE = 16;
    public static final int CORE_POOL_SIZE = 2;

    private final ExecutorService executorService;

    private ThreadPoolManager() {
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
    }

    public static ThreadPoolManager getInstance() {
        return INSTANCE;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

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