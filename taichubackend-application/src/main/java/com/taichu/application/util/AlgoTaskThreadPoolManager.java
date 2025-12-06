package com.taichu.application.util;

import com.taichu.common.common.util.RequestContext;
import com.taichu.domain.enums.AlgoTaskTypeEnum;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class AlgoTaskThreadPoolManager {
    // 各任务类型的并发配置
    private static final int SCRIPT_GENERATION_MAX_CONCURRENCY = 100;
    private static final int ROLE_IMG_GENERATION_MAX_CONCURRENCY = 100;
    private static final int STORYBOARD_TEXT_GENERATION_MAX_CONCURRENCY = 100;
    private static final int STORYBOARD_IMG_GENERATION_MAX_CONCURRENCY = 5;
    private static final int STORYBOARD_VIDEO_GENERATION_MAX_CONCURRENCY = 5;
    private static final int FULL_VIDEO_GENERATION_MAX_CONCURRENCY = 10;
    
    // 线程池通用配置
    private static final long KEEP_ALIVE_TIME = 60L;
    private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;
    private static final int QUEUE_CAPACITY = 500;
    
    private static volatile AlgoTaskThreadPoolManager INSTANCE;
    private final Map<AlgoTaskTypeEnum, ExecutorService> executorServiceMap = new HashMap<>();

    private AlgoTaskThreadPoolManager() {
        log.info("Creating new AlgoTaskThreadPoolManager instance...");
        initializeThreadPools();
        log.info("AlgoTaskThreadPoolManager instance created successfully with {} thread pools", executorServiceMap.size());
    }
    
    /**
     * 初始化所有任务类型的线程池
     */
    private void initializeThreadPools() {
        // 生成剧本接口：最大并发100
        createThreadPool(AlgoTaskTypeEnum.SCRIPT_GENERATION, 
                SCRIPT_GENERATION_MAX_CONCURRENCY, "script-generation");
        
        // 角色图生成接口：最大并发100
        createThreadPool(AlgoTaskTypeEnum.ROLE_IMG_GENERATION, 
                ROLE_IMG_GENERATION_MAX_CONCURRENCY, "role-img-generation");
        
        // 分镜文本生成：最大并发100
        createThreadPool(AlgoTaskTypeEnum.STORYBOARD_TEXT_GENERATION, 
                STORYBOARD_TEXT_GENERATION_MAX_CONCURRENCY, "storyboard-text-generation");
        
        // 分镜图生成：最大并发量=5
        createThreadPool(AlgoTaskTypeEnum.STORYBOARD_IMG_GENERATION, 
                STORYBOARD_IMG_GENERATION_MAX_CONCURRENCY, "storyboard-img-generation");
        
        // 分镜视频生成：最大并发量=5
        createThreadPool(AlgoTaskTypeEnum.STORYBOARD_VIDEO_GENERATION, 
                STORYBOARD_VIDEO_GENERATION_MAX_CONCURRENCY, "storyboard-video-generation");
        
        // 合成最终接口：最大并发=10
        createThreadPool(AlgoTaskTypeEnum.FULL_VIDEO_GENERATION, 
                FULL_VIDEO_GENERATION_MAX_CONCURRENCY, "full-video-generation");
        
        // 用户重试任务类型映射到对应的主任务类型线程池
        // 用户重试单个分镜图片生成 -> 分镜图生成线程池
        executorServiceMap.put(AlgoTaskTypeEnum.USER_RETRY_SINGLE_STORYBOARD_IMG_GENERATION,
                executorServiceMap.get(AlgoTaskTypeEnum.STORYBOARD_IMG_GENERATION));
        
        // 用户重试单个分镜视频生成 -> 分镜视频生成线程池
        executorServiceMap.put(AlgoTaskTypeEnum.USER_RETRY_SINGLE_STORYBOARD_VIDEO_GENERATION,
                executorServiceMap.get(AlgoTaskTypeEnum.STORYBOARD_VIDEO_GENERATION));
        
        // 用户重试完整视频生成 -> 完整视频生成线程池
        executorServiceMap.put(AlgoTaskTypeEnum.USER_RETRY_FULL_VIDEO_GENERATION,
                executorServiceMap.get(AlgoTaskTypeEnum.FULL_VIDEO_GENERATION));
    }
    
    /**
     * 为指定任务类型创建线程池
     */
    private void createThreadPool(AlgoTaskTypeEnum taskType, int maxConcurrency, String poolName) {
        int corePoolSize = Math.min(5, maxConcurrency);  // 核心线程数 = min(5, 最大并发数)
        ExecutorService executor = new ThreadPoolExecutor(
                corePoolSize,                          // 核心线程数
                maxConcurrency,                        // 最大线程数
                KEEP_ALIVE_TIME,                       // 空闲线程存活时间
                TIME_UNIT,                             // 时间单位
                new LinkedBlockingQueue<>(QUEUE_CAPACITY),  // 工作队列
                new ThreadFactory() {
                    private final AtomicInteger threadNumber = new AtomicInteger(1);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, poolName + "-" + threadNumber.getAndIncrement());
                        t.setDaemon(true);  // 设置为守护线程
                        return t;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略
        );
        executorServiceMap.put(taskType, executor);
        log.info("Created thread pool for task type: {}, core pool size: {}, max concurrency: {}, pool name: {}", 
                taskType, corePoolSize, maxConcurrency, poolName);
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

    /**
     * 根据任务类型获取对应的线程池
     */
    private ExecutorService getExecutorService(AlgoTaskTypeEnum taskType) {
        if (INSTANCE == null) {
            log.error("AlgoTaskThreadPoolManager not initialized. Please call init() first.");
            throw new IllegalStateException("AlgoTaskThreadPoolManager not initialized. Please call init() first.");
        }
        
        ExecutorService executor = executorServiceMap.get(taskType);
        if (executor == null) {
            log.error("No thread pool found for task type: {}. Available types: {}", taskType, executorServiceMap.keySet());
            throw new IllegalArgumentException("No thread pool found for task type: " + taskType);
        }
        return executor;
    }

    /**
     * 执行任务（根据任务类型选择对应的线程池）
     * 
     * @param taskType 任务类型
     * @param task 要执行的任务
     */
    public void execute(AlgoTaskTypeEnum taskType, Runnable task) {
        String requestId = RequestContext.getRequestId();
        ExecutorService executor = getExecutorService(taskType);
        executor.execute(() -> {
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

    /**
     * 提交任务（根据任务类型选择对应的线程池）
     * 
     * @param taskType 任务类型
     * @param task 要提交的任务
     * @return Future对象
     */
    public <T> Future<T> submit(AlgoTaskTypeEnum taskType, Callable<T> task) {
        String requestId = RequestContext.getRequestId();
        ExecutorService executor = getExecutorService(taskType);
        return executor.submit(() -> {
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
    
    /**
     * 兼容旧版本的execute方法（使用默认线程池，已废弃）
     * @deprecated 请使用 {@link #execute(AlgoTaskTypeEnum, Runnable)} 方法
     */
    @Deprecated
    public void execute(Runnable task) {
        log.warn("Using deprecated execute(Runnable) method. Please use execute(AlgoTaskTypeEnum, Runnable) instead.");
        // 使用第一个可用的线程池作为默认线程池
        if (executorServiceMap.isEmpty()) {
            throw new IllegalStateException("No thread pools available");
        }
        AlgoTaskTypeEnum defaultType = executorServiceMap.keySet().iterator().next();
        execute(defaultType, task);
    }
    
    /**
     * 兼容旧版本的submit方法（使用默认线程池，已废弃）
     * @deprecated 请使用 {@link #submit(AlgoTaskTypeEnum, Callable)} 方法
     */
    @Deprecated
    public <T> Future<T> submit(Callable<T> task) {
        log.warn("Using deprecated submit(Callable) method. Please use submit(AlgoTaskTypeEnum, Callable) instead.");
        // 使用第一个可用的线程池作为默认线程池
        if (executorServiceMap.isEmpty()) {
            throw new IllegalStateException("No thread pools available");
        }
        AlgoTaskTypeEnum defaultType = executorServiceMap.keySet().iterator().next();
        return submit(defaultType, task);
    }

    /**
     * 关闭所有线程池
     */
    public static void shutdown() {
        log.info("Shutting down AlgoTaskThreadPoolManager...");
        if (INSTANCE != null && !INSTANCE.executorServiceMap.isEmpty()) {
            int poolCount = 0;
            for (Map.Entry<AlgoTaskTypeEnum, ExecutorService> entry : INSTANCE.executorServiceMap.entrySet()) {
                // 避免重复关闭（用户重试任务类型共享线程池）
                if (entry.getKey() == AlgoTaskTypeEnum.USER_RETRY_SINGLE_STORYBOARD_IMG_GENERATION ||
                    entry.getKey() == AlgoTaskTypeEnum.USER_RETRY_SINGLE_STORYBOARD_VIDEO_GENERATION ||
                    entry.getKey() == AlgoTaskTypeEnum.USER_RETRY_FULL_VIDEO_GENERATION) {
                    continue; // 跳过用户重试类型，它们共享主任务类型的线程池
                }
                
                ExecutorService executor = entry.getValue();
                if (executor != null && !executor.isShutdown()) {
                    try {
                        executor.shutdown();
                        if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                            executor.shutdownNow();
                            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                                log.warn("Thread pool for {} did not terminate", entry.getKey());
                            }
                        }
                        poolCount++;
                        log.info("Thread pool for {} shut down successfully", entry.getKey());
                    } catch (InterruptedException e) {
                        executor.shutdownNow();
                        Thread.currentThread().interrupt();
                        log.error("Thread pool shutdown interrupted for {}", entry.getKey(), e);
                    }
                }
            }
            log.info("AlgoTaskThreadPoolManager shut down successfully, closed {} thread pools", poolCount);
        } else {
            log.info("AlgoTaskThreadPoolManager was not initialized, nothing to shut down");
        }
    }
} 