package com.taichu.application.executor;

import com.alibaba.cola.dto.SingleResponse;
import com.taichu.application.util.ThreadPoolManager;
import com.taichu.domain.enums.TaskStatusEnum;
import com.taichu.domain.enums.TaskTypeEnum;
import com.taichu.domain.enums.WorkflowStatusEnum;
import com.taichu.domain.model.FicWorkflowTaskBO;
import com.taichu.infra.repo.FicWorkflowRepository;
import com.taichu.infra.repo.FicWorkflowTaskRepository;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public abstract class AbstractTaskExecutor implements TaskExecutor {
    protected final FicWorkflowRepository workflowRepository;
    protected final FicWorkflowTaskRepository ficWorkflowTaskRepository;

    public AbstractTaskExecutor(FicWorkflowRepository workflowRepository, FicWorkflowTaskRepository ficWorkflowTaskRepository) {
        this.workflowRepository = workflowRepository;
        this.ficWorkflowTaskRepository = ficWorkflowTaskRepository;
    }

    public SingleResponse<Long> submitTask(Long workflowId, Object request) {
        try {
            // 1. 更新工作流状态
            workflowRepository.updateStatus(workflowId, getInitWorkflowStatus().getCode());

            // 2. 创建任务记录
            FicWorkflowTaskBO ficWorkflowTaskBO = new FicWorkflowTaskBO();
            ficWorkflowTaskBO.setWorkflowId(workflowId);
            ficWorkflowTaskBO.setGmtCreate(System.currentTimeMillis());
            ficWorkflowTaskBO.setTaskType(getWorkflowTaskType().name());
            ficWorkflowTaskBO.setStatus(TaskStatusEnum.RUNNING.getCode());
            Map<String, String> params = constructTaskParams(workflowId, request);
            ficWorkflowTaskBO.setParams(params);
            // 写入DB
            long workflowTaskId = ficWorkflowTaskRepository.createFicWorkflowTask(ficWorkflowTaskBO);
            // 回写workflowTaskId
            ficWorkflowTaskBO.setId(workflowTaskId);

            // 3. 提交后台任务到线程池
            submit(() -> {
                backgroundProcessing(ficWorkflowTaskBO);
                return null;
            });

            // 4. 返回任务id给前端
            return SingleResponse.of(workflowTaskId);
        } catch (Exception e) {
            getLog().error("Failed to submit script task for workflow: " + workflowId, e);
            // 如果任务记录都还没创建就失败了，只需要回滚工作流状态
            workflowRepository.updateStatus(workflowId, getRollbackWorkflowStatus().getCode());
            // 将所有进行中的workflowTask置为失败
            List<FicWorkflowTaskBO> workflowTasks = ficWorkflowTaskRepository.findByWorkflowId(workflowId);
            workflowTasks.stream()
                    .filter(task -> TaskStatusEnum.RUNNING.getCode().equals(task.getStatus()))
                    .forEach(task -> ficWorkflowTaskRepository.updateTaskStatus(task.getId(), TaskStatusEnum.FAILED));

            return SingleResponse.buildFailure("SCRIPT_001", "提交剧本生成任务失败: " + e.getMessage());
        }
    }

    private <T> void submit(Callable<T> task) {
        ThreadPoolManager.getInstance().submit(task);
    }

    protected abstract Logger getLog();

    protected void backgroundProcessing(FicWorkflowTaskBO task) {
        Long workflowId = task.getWorkflowId();
        try {
            doStartBackgroundProcessing(task);
            markWorkflowTaskAsSuccess(task);
        } catch (Exception e) {
            getLog().error("Background processing failed for workflow: " + task.getWorkflowId(), e);
            workflowRepository.updateStatus(workflowId, getRollbackWorkflowStatus().getCode());
            // 做后置处理，处理多种algo任务中有任何一个失败了进行的回滚
            doWhileBackgroundProcessingFail(task);
            return;
        }

        workflowRepository.updateStatus(workflowId, getDoneWorkflowStatus().getCode());
    }

    protected void doWhileBackgroundProcessingFail(FicWorkflowTaskBO task) {}


    protected void markWorkflowTaskAsSuccess(FicWorkflowTaskBO workflowTask) {
        // 更新工作流任务状态为成功
        workflowTask.setStatus(TaskStatusEnum.COMPLETED.getCode());
        ficWorkflowTaskRepository.updateTaskStatus(workflowTask.getId(), TaskStatusEnum.COMPLETED);
        getLog().info("All algo tasks completed successfully for workflow: " + workflowTask.getWorkflowId());
    }

    protected abstract void doStartBackgroundProcessing(FicWorkflowTaskBO task) throws Exception;

    public abstract WorkflowStatusEnum getInitWorkflowStatus();

    public abstract WorkflowStatusEnum getDoneWorkflowStatus();

    public abstract WorkflowStatusEnum getRollbackWorkflowStatus();

    public abstract TaskTypeEnum getWorkflowTaskType();

    protected abstract Map<String, String> constructTaskParams(Long workflowId, Object request);
}
