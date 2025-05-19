package com.taichu.domain.model;

import lombok.Data;

/**
 * 任务业务对象
 */
@Data
public class FicTaskBO {
    
    /**
     * 任务ID
     */
    private Long id;

    /**
     * 工作流ID
     */
    private Long workflowId;

    /**
     * 任务类型
     */
    private String taskType;

    /**
     * 任务状态：0-失败，1-执行中，2-成功
     */
    private Byte status;

    /**
     * 算法任务ID
     */
    private Long algoTaskId;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 更新时间
     */
    private Long updateTime;
} 