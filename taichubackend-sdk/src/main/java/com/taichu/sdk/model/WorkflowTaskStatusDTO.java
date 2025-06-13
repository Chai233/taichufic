package com.taichu.sdk.model;

import lombok.Data;

/**
 * 任务状态数据传输对象
 */
@Data
public class WorkflowTaskStatusDTO {
    
    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 任务状态
     */
    private String status;

    /**
     * 进度百分比
     */
    private Integer progressRatio;

}
