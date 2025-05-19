package com.taichu.domain.gateway;

import com.taichu.domain.model.AlgoResponse;
import com.taichu.domain.model.AlgoResult;
import com.taichu.domain.model.TaskStatus;

/**
 * 算法服务网关接口
 */
public interface AlgoGateway {
    /**
     * 提交剧本生成任务
     *
     * @param workflowId 工作流ID
     * @return 算法任务响应
     */
    AlgoResponse submitScriptTask(Long workflowId);

    /**
     * 提交分镜生成任务
     *
     * @param workflowId 工作流ID
     * @return 算法任务响应
     */
    AlgoResponse submitStoryboardTask(Long workflowId);

    /**
     * 检查任务状态
     *
     * @param taskId 任务ID
     * @return 任务状态
     */
    TaskStatus checkTaskStatus(Long taskId);

    /**
     * 获取任务结果
     *
     * @param workflowId 工作流ID
     * @return 任务结果
     */
    AlgoResult getTaskResult(Long workflowId);
} 