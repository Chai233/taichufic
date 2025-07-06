package com.taichu.application.service;

import com.taichu.application.applicationrun.StartupRecoveryService;
import com.taichu.application.executor.TaskExecutorFactory;
import com.taichu.application.executor.TaskExecutor;
import com.taichu.domain.enums.TaskStatusEnum;
import com.taichu.domain.enums.WorkflowStatusEnum;
import com.taichu.domain.model.FicAlgoTaskBO;
import com.taichu.domain.model.FicWorkflowTaskBO;
import com.taichu.infra.repo.FicAlgoTaskRepository;
import com.taichu.infra.repo.FicWorkflowRepository;
import com.taichu.infra.repo.FicWorkflowTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * StartupRecoveryService 测试类
 */
@ExtendWith(MockitoExtension.class)
class StartupRecoveryServiceTest {

    @Mock
    private FicWorkflowTaskRepository ficWorkflowTaskRepository;

    @Mock
    private FicAlgoTaskRepository ficAlgoTaskRepository;

    @Mock
    private FicWorkflowRepository ficWorkflowRepository;

    @Mock
    private TaskExecutorFactory taskExecutorFactory;

    @Mock
    private TaskExecutor mockTaskExecutor;

    @Mock
    private ApplicationArguments applicationArguments;

    @InjectMocks
    private StartupRecoveryService startupRecoveryService;

    private FicWorkflowTaskBO mockWorkflowTask;
    private FicAlgoTaskBO mockAlgoTask;

    @BeforeEach
    void setUp() {
        // 创建模拟的工作流任务
        mockWorkflowTask = new FicWorkflowTaskBO();
        mockWorkflowTask.setId(1L);
        mockWorkflowTask.setWorkflowId(100L);
        mockWorkflowTask.setTaskType("SCRIPT_AND_ROLE_GENERATION");
        mockWorkflowTask.setStatus(TaskStatusEnum.RUNNING.getCode());

        // 创建模拟的算法任务
        mockAlgoTask = new FicAlgoTaskBO();
        mockAlgoTask.setId(1L);
        mockAlgoTask.setWorkflowTaskId(1L);
        mockAlgoTask.setStatus(TaskStatusEnum.RUNNING.getCode());
        mockAlgoTask.setTaskType("SCRIPT_GENERATION");
    }

    @Test
    void testRunWithNoRunningTasks() throws Exception {
        // 设置模拟行为：没有运行中的任务
        when(ficWorkflowTaskRepository.findRunningTasks()).thenReturn(Collections.emptyList());

        // 执行测试
        startupRecoveryService.run(applicationArguments);

        // 验证调用
        verify(ficWorkflowTaskRepository).findRunningTasks();
        verify(ficAlgoTaskRepository, never()).findByWorkflowTaskId(any());
        verify(ficWorkflowRepository, never()).updateStatus(any(), any());
    }

    @Test
    void testRunWithRunningTasks() throws Exception {
        // 设置模拟行为：有运行中的任务
        List<FicWorkflowTaskBO> runningTasks = Arrays.asList(mockWorkflowTask);
        when(ficWorkflowTaskRepository.findRunningTasks()).thenReturn(runningTasks);
        when(ficAlgoTaskRepository.findByWorkflowTaskId(1L)).thenReturn(Arrays.asList(mockAlgoTask));
        when(taskExecutorFactory.getExecutor("SCRIPT_AND_ROLE_GENERATION")).thenReturn(mockTaskExecutor);
        when(mockTaskExecutor.getRollbackWorkflowStatus()).thenReturn(WorkflowStatusEnum.UPLOAD_FILE_DONE);

        // 执行测试
        startupRecoveryService.run(applicationArguments);

        // 验证调用
        verify(ficWorkflowTaskRepository).findRunningTasks();
        verify(ficWorkflowTaskRepository).updateTaskStatus(1L, TaskStatusEnum.FAILED);
        verify(ficAlgoTaskRepository).findByWorkflowTaskId(1L);
        verify(ficAlgoTaskRepository).updateStatus(1L, TaskStatusEnum.FAILED);
        verify(taskExecutorFactory).getExecutor("SCRIPT_AND_ROLE_GENERATION");
        verify(mockTaskExecutor).getRollbackWorkflowStatus();
        verify(ficWorkflowRepository).updateStatus(100L, WorkflowStatusEnum.UPLOAD_FILE_DONE.getCode());
    }

    @Test
    void testRunWithException() throws Exception {
        // 设置模拟行为：抛出异常
        when(ficWorkflowTaskRepository.findRunningTasks()).thenThrow(new RuntimeException("Database error"));

        // 执行测试 - 不应该抛出异常
        startupRecoveryService.run(applicationArguments);

        // 验证调用
        verify(ficWorkflowTaskRepository).findRunningTasks();
        verify(ficAlgoTaskRepository, never()).findByWorkflowTaskId(any());
        verify(ficWorkflowRepository, never()).updateStatus(any(), any());
    }

    @Test
    void testDetermineRollbackStatus() throws Exception {
        // 测试不同的任务类型对应的回滚状态
        // 通过实际运行来测试整个流程
        
        List<FicWorkflowTaskBO> runningTasks = Arrays.asList(mockWorkflowTask);
        when(ficWorkflowTaskRepository.findRunningTasks()).thenReturn(runningTasks);
        when(ficAlgoTaskRepository.findByWorkflowTaskId(1L)).thenReturn(Collections.emptyList());
        when(taskExecutorFactory.getExecutor("SCRIPT_AND_ROLE_GENERATION")).thenReturn(mockTaskExecutor);
        when(mockTaskExecutor.getRollbackWorkflowStatus()).thenReturn(WorkflowStatusEnum.UPLOAD_FILE_DONE);

        // 执行测试
        startupRecoveryService.run(applicationArguments);

        // 验证工作流状态被回滚到正确的状态
        verify(taskExecutorFactory).getExecutor("SCRIPT_AND_ROLE_GENERATION");
        verify(mockTaskExecutor).getRollbackWorkflowStatus();
        verify(ficWorkflowRepository).updateStatus(100L, WorkflowStatusEnum.UPLOAD_FILE_DONE.getCode());
    }
} 