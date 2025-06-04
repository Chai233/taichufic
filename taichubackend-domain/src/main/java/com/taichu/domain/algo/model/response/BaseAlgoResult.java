package com.taichu.domain.algo.model.response;

import lombok.Data;

/**
 * 算法服务响应结果基类
 * 包含所有结果类共有的字段
 */
@Data
public class BaseAlgoResult {
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 错误码
     */
    private String errorCode;
    
    /**
     * 错误信息
     */
    private String errorMsg;
} 