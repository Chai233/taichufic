package com.taichu.application.service.inner.algo;

import com.taichu.domain.enums.AlgoTaskTypeEnum;
import com.taichu.domain.enums.TaskStatusEnum;
import com.taichu.domain.model.FicAlgoTaskBO;
import com.taichu.domain.model.FicWorkflowTaskBO;

import java.util.List;

/**
 * 算法任务处理器接口
 */
public interface AlgoTaskProcessor {

    AlgoTaskTypeEnum getAlgoTaskType();

    /**
     * 生成阶段：创建所有算法任务
     */
    List<AlgoTaskBO> generateTasks(FicWorkflowTaskBO workflowTask) throws Exception;

    /**
     * 状态检查阶段：检查任务状态
     */
    TaskStatusEnum checkSingleTaskStatus(FicAlgoTaskBO algoTask);

    /**
     * 单任务成功后置处理
     * @param algoTask
     */
    void singleTaskSuccessPostProcess(FicAlgoTaskBO algoTask) throws Exception;

    /**
     * 后置处理阶段：处理任务完成后的业务逻辑
     */
    void postProcessAllComplete(FicWorkflowTaskBO workflowTask, List<FicAlgoTaskBO> algoTasks);

    /**
     * 后置处理阶段：处理任务完成后的业务逻辑
     */
    void postProcessAnyFailed(FicWorkflowTaskBO workflowTask, List<FicAlgoTaskBO> algoTasks);
}
