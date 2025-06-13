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
import java.util.concurrent.ExecutorService;

public abstract class AbstractTaskExecutor {
    protected final FicWorkflowRepository workflowRepository;
    protected final FicWorkflowTaskRepository ficWorkflowTaskRepository;
    private final ExecutorService executorService = ThreadPoolManager.getInstance().getExecutorService();

    public AbstractTaskExecutor(FicWorkflowRepository workflowRepository, FicWorkflowTaskRepository ficWorkflowTaskRepository) {
        this.workflowRepository = workflowRepository;
        this.ficWorkflowTaskRepository = ficWorkflowTaskRepository;
    }

    public SingleResponse<Long> submitTask(Long workflowId, Object request) {
        try {
            // 1. 更新工作流状态
            workflowRepository.updateStatus(workflowId, getNewWorkflowStatus().getCode());

            // 2. 创建任务记录
            FicWorkflowTaskBO ficWorkflowTaskBO = new FicWorkflowTaskBO();
            ficWorkflowTaskBO.setWorkflowId(workflowId);
            ficWorkflowTaskBO.setGmtCreate(System.currentTimeMillis());
            ficWorkflowTaskBO.setTaskType(getWorkflowTaskType().name());
            ficWorkflowTaskBO.setStatus(TaskStatusEnum.RUNNING.getCode());
            long workflowTaskId = ficWorkflowTaskRepository.createFicWorkflowTask(ficWorkflowTaskBO);
            ficWorkflowTaskBO.setId(workflowTaskId);
            Map<String, String> params = constructTaskParams(workflowId, request);
            ficWorkflowTaskBO.setParams(params);

            // 3. 提交后台任务到线程池
            executorService.submit(() -> startBackgroundProcessing(ficWorkflowTaskBO));

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

    protected abstract Logger getLog();

    protected abstract void startBackgroundProcessing(FicWorkflowTaskBO task);

    protected abstract WorkflowStatusEnum getNewWorkflowStatus();

    protected abstract WorkflowStatusEnum getRollbackWorkflowStatus();

    protected abstract TaskTypeEnum getWorkflowTaskType();

    protected abstract Map<String, String> constructTaskParams(Long workflowId, Object request);
}
