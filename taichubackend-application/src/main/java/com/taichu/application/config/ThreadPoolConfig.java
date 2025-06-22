package com.taichu.application.config;

import com.taichu.application.util.ThreadPoolManager;
import com.taichu.application.util.AlgoTaskThreadPoolManager;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Configuration
public class ThreadPoolConfig {
    private static final Logger log = LoggerFactory.getLogger(ThreadPoolConfig.class);
    
    @PostConstruct
    public void init() {
        log.info("Initializing ThreadPoolManager...");
        try {
            ThreadPoolManager.init();
            log.info("ThreadPoolManager initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize ThreadPoolManager", e);
            throw e;
        }
        
        log.info("Initializing AlgoTaskThreadPoolManager...");
        try {
            AlgoTaskThreadPoolManager.init();
            log.info("AlgoTaskThreadPoolManager initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize AlgoTaskThreadPoolManager", e);
            throw e;
        }
    }
    
    @PreDestroy
    public void destroy() {
        log.info("Shutting down ThreadPoolManager...");
        try {
            ThreadPoolManager.shutdown();
            log.info("ThreadPoolManager shut down successfully");
        } catch (Exception e) {
            log.error("Error while shutting down ThreadPoolManager", e);
        }
        
        log.info("Shutting down AlgoTaskThreadPoolManager...");
        try {
            AlgoTaskThreadPoolManager.shutdown();
            log.info("AlgoTaskThreadPoolManager shut down successfully");
        } catch (Exception e) {
            log.error("Error while shutting down AlgoTaskThreadPoolManager", e);
        }
    }
} 