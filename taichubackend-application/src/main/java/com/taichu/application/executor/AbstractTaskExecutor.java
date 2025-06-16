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
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public abstract class AbstractTaskExecutor {
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
            getExecutorService().submit(() -> startBackgroundProcessing(ficWorkflowTaskBO));

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

    private ExecutorService getExecutorService() {
        return ThreadPoolManager.getInstance().getExecutorService();
    }

    protected abstract Logger getLog();

    protected void startBackgroundProcessing(FicWorkflowTaskBO task) {
        Long workflowId = task.getWorkflowId();
        try {
            doStartBackgroundProcessing(task);
        } catch (Exception e) {
            getLog().error("Background processing failed for workflow: " + task.getWorkflowId(), e);
            workflowRepository.updateStatus(workflowId, getRollbackWorkflowStatus().getCode());
            return;
        }

        workflowRepository.updateStatus(workflowId, getDoneWorkflowStatus().getCode());
    }

    protected abstract void doStartBackgroundProcessing(FicWorkflowTaskBO task);

    protected abstract WorkflowStatusEnum getInitWorkflowStatus();

    protected abstract WorkflowStatusEnum getDoneWorkflowStatus();

    protected abstract WorkflowStatusEnum getRollbackWorkflowStatus();

    protected abstract TaskTypeEnum getWorkflowTaskType();

    protected abstract Map<String, String> constructTaskParams(Long workflowId, Object request);
}
