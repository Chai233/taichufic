package com.taichu.infra.http;

/**
 * 算法服务 HTTP 异常类
 * 用于封装与算法服务通信过程中的 HTTP 相关异常
 */
public class AlgoHttpException extends RuntimeException {
    
    private final Integer statusCode;
    
    public AlgoHttpException(String message) {
        super(message);
        this.statusCode = null;
    }
    
    public AlgoHttpException(String message, Integer statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
    
    public AlgoHttpException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
    }
    
    public AlgoHttpException(String message, Integer statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }
    
    public Integer getStatusCode() {
        return statusCode;
    }
} 