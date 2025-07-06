package com.taichu.application.applicationrun;

import com.taichu.application.executor.TaskExecutorFactory;
import com.taichu.domain.enums.TaskStatusEnum;
import com.taichu.domain.enums.WorkflowStatusEnum;
import com.taichu.domain.model.FicAlgoTaskBO;
import com.taichu.domain.model.FicWorkflowTaskBO;
import com.taichu.infra.repo.FicAlgoTaskRepository;
import com.taichu.infra.repo.FicWorkflowRepository;
import com.taichu.infra.repo.FicWorkflowTaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 启动恢复服务
 * 用于在应用启动时处理所有运行中的任务，将它们标记为失败并回滚工作流状态
 */
@Slf4j
@Service
public class StartupRecoveryService implements ApplicationRunner {

    @Autowired
    private FicWorkflowTaskRepository ficWorkflowTaskRepository;

    @Autowired
    private FicAlgoTaskRepository ficAlgoTaskRepository;

    @Autowired
    private FicWorkflowRepository ficWorkflowRepository;

    @Autowired
    private TaskExecutorFactory taskExecutorFactory;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("开始执行启动恢复任务...");
        
        try {
            // 1. 查找所有运行中的工作流任务
            List<FicWorkflowTaskBO> runningWorkflowTasks = findRunningWorkflowTasks();
            log.info("发现 {} 个运行中的工作流任务", runningWorkflowTasks.size());
            
            if (runningWorkflowTasks.isEmpty()) {
                log.info("没有发现运行中的工作流任务，启动恢复完成");
                return;
            }
            
            // 2. 处理每个运行中的工作流任务
            for (FicWorkflowTaskBO workflowTask : runningWorkflowTasks) {
                processRunningWorkflowTask(workflowTask);
            }
            
            log.info("启动恢复任务执行完成，共处理了 {} 个运行中的工作流任务", runningWorkflowTasks.size());
            
        } catch (Exception e) {
            log.error("启动恢复任务执行失败", e);
            // 不抛出异常，避免影响应用启动
        }
    }

    /**
     * 查找所有运行中的工作流任务
     */
    private List<FicWorkflowTaskBO> findRunningWorkflowTasks() {
        return ficWorkflowTaskRepository.findRunningTasks();
    }

    /**
     * 处理运行中的工作流任务
     */
    private void processRunningWorkflowTask(FicWorkflowTaskBO workflowTask) {
        Long workflowId = workflowTask.getWorkflowId();
        Long workflowTaskId = workflowTask.getId();
        
        log.info("处理运行中的工作流任务: workflowId={}, workflowTaskId={}, taskType={}", 
                workflowId, workflowTaskId, workflowTask.getTaskType());
        
        try {
            // 1. 将工作流任务标记为失败
            ficWorkflowTaskRepository.updateTaskStatus(workflowTaskId, TaskStatusEnum.FAILED);
            log.info("已将工作流任务标记为失败: workflowTaskId={}", workflowTaskId);
            
            // 2. 查找并处理相关的算法任务
            List<FicAlgoTaskBO> algoTasks = ficAlgoTaskRepository.findByWorkflowTaskId(workflowTaskId);
            int runningAlgoTaskCount = 0;
            
            for (FicAlgoTaskBO algoTask : algoTasks) {
                if (TaskStatusEnum.RUNNING.getCode().equals(algoTask.getStatus())) {
                    ficAlgoTaskRepository.updateStatus(algoTask.getId(), TaskStatusEnum.FAILED);
                    runningAlgoTaskCount++;
                    log.info("已将算法任务标记为失败: algoTaskId={}", algoTask.getId());
                }
            }
            
            log.info("处理了 {} 个运行中的算法任务", runningAlgoTaskCount);
            
            // 3. 回滚工作流状态
            WorkflowStatusEnum rollbackStatus = determineRollbackStatus(workflowTask.getTaskType());
            ficWorkflowRepository.updateStatus(workflowId, rollbackStatus.getCode());
            log.info("已将工作流状态回滚到: workflowId={}, status={}", workflowId, rollbackStatus.getDescription());
            
        } catch (Exception e) {
            log.error("处理运行中的工作流任务失败: workflowTaskId={}", workflowTaskId, e);
        }
    }

    /**
     * 根据任务类型确定回滚状态
     */
    private WorkflowStatusEnum determineRollbackStatus(String taskType) {
        // 通过工厂类获取对应的执行器，然后获取回滚状态
        var executor = taskExecutorFactory.getExecutor(taskType);
        if (executor != null) {
            WorkflowStatusEnum rollbackStatus = executor.getRollbackWorkflowStatus();
            log.info("通过执行器获取回滚状态: taskType={}, rollbackStatus={}", taskType, rollbackStatus.getDescription());
            return rollbackStatus;
        } else {
            log.warn("未找到任务类型 {} 对应的执行器，回滚到初始状态", taskType);
            return WorkflowStatusEnum.INIT_WAIT_FOR_FILE;
        }
    }
} 