package com.taichu.sdk.model;

import lombok.Data;

/**
 * 任务状态数据传输对象
 */
@Data
public class TaskStatusDTO {
    
    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 任务状态：0-失败，1-执行中，2-成功
     */
    private Byte status;
}
