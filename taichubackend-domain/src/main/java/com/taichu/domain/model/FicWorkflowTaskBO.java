package com.taichu.domain.model;

import lombok.Data;

/**
 * 任务业务对象
 */
@Data
public class FicWorkflowTaskBO {
    /**
     *  任务id
     */
    private Long id;

    /**
     * 创建时间
     */
    private Long gmtCreate;

    /**
     * workflowid
     */
    private Long workflowId;

    /**
     * 状态
     * @see com.taichu.domain.enums.TaskStatusEnum
     */
    private Byte status;

    /**
     *
     * 任务类型
     * @see com.taichu.domain.enums.TaskTypeEnum
     */
    private String taskType;
} 