package com.taichu.application.executor;

import com.taichu.domain.enums.TaskTypeEnum;
import com.taichu.domain.enums.WorkflowStatusEnum;

/**
 * 任务执行器接口
 * 定义任务执行器的核心方法
 */
public interface TaskExecutor {
    
    /**
     * 获取任务类型
     * @return 任务类型枚举
     */
    TaskTypeEnum getWorkflowTaskType();
    
    /**
     * 获取初始化工作流状态
     * @return 初始化工作流状态
     */
    WorkflowStatusEnum getInitWorkflowStatus();
    
    /**
     * 获取完成工作流状态
     * @return 完成工作流状态
     */
    WorkflowStatusEnum getDoneWorkflowStatus();
    
    /**
     * 获取回滚工作流状态
     * @return 回滚工作流状态
     */
    WorkflowStatusEnum getRollbackWorkflowStatus();
} 