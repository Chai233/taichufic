package com.taichu.domain.algo.model;

import lombok.Data;

@Data
public class AlgoResponse {
    private String taskId;      // 任务ID
    private boolean isSuccess;  // 是否成功
    private String errorCode;   // 错误码
    private String errorMsg;    // 错误信息
} 