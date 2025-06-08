package com.taichu.domain.model;

import lombok.Data;

/**
 * 任务业务对象
 */
@Data
public class FicAlgoTaskBO {
    
    /**
     * 任务ID
     */
    private Long id;

    /**
     * 创建时间
     */
    private Long gmtCreate;

    /**
     * 工作流任务ID
     */
    private Long workflowTaskId;

    /**
     * 任务状态：0-失败，1-执行中，2-成功
     */
    private Byte status;

    /**
     * 任务类型
     */
    private String taskType;    

    /**
     * 算法任务ID
     */
    private Long algoTaskId;

    /**
     * 错误信息
     */
    private String taskAbstract;
} 