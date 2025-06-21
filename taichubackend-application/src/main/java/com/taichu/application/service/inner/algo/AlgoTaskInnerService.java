package com.taichu.application.service.inner.algo;

import com.taichu.domain.enums.AlgoTaskTypeEnum;
import com.taichu.domain.enums.TaskStatusEnum;
import com.taichu.domain.model.FicAlgoTaskBO;
import com.taichu.domain.model.FicWorkflowTaskBO;
import com.taichu.infra.repo.FicAlgoTaskRepository;
import com.taichu.infra.repo.FicWorkflowTaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AlgoTaskInnerService implements InitializingBean {

    public static final int WAIT_INTERVAL_MILLIS = 30000;
    private final FicAlgoTaskRepository ficAlgoTaskRepository;
    private final List<AlgoTaskProcessor> taskProcessors = new ArrayList<>();
    private final Map<AlgoTaskTypeEnum, AlgoTaskProcessor> taskProcessorMap = new ConcurrentHashMap<>();
    private final FicWorkflowTaskRepository ficWorkflowTaskRepository;

    public AlgoTaskInnerService(FicAlgoTaskRepository ficAlgoTaskRepository, List<AlgoTaskProcessor> algoTaskProcessors, FicWorkflowTaskRepository ficWorkflowTaskRepository) {
        this.ficAlgoTaskRepository = ficAlgoTaskRepository;
        this.taskProcessors.addAll(algoTaskProcessors);
        this.ficWorkflowTaskRepository = ficWorkflowTaskRepository;
    }

    /**
     * 运行算法任务
     */
    public void runAlgoTask(FicWorkflowTaskBO ficWorkflowTaskBO, AlgoTaskTypeEnum algoTaskTypeEnum) {
        Long workflowTaskId = ficWorkflowTaskBO.getId();
        log.info("[runAlgoTask] 开始运行算法任务, workflowTaskId: {}, algoTaskType: {}", workflowTaskId, algoTaskTypeEnum);
        try {

            // 1. 获取任务处理器
            AlgoTaskProcessor processor = taskProcessorMap.get(algoTaskTypeEnum);
            if (processor == null) {
                log.error("[runAlgoTask] 未找到任务类型[{}]的处理器", algoTaskTypeEnum);
                return;
            }
            log.info("[runAlgoTask] 获取到处理器: {}", processor.getClass().getSimpleName());

            // 2. 生成阶段：创建算法任务
            List<AlgoTaskBO> algoTaskBOList = processor.generateTasks(ficWorkflowTaskBO);
            log.info("[runAlgoTask] 生成算法任务: {}", algoTaskBOList);
            if (algoTaskBOList == null || algoTaskBOList.isEmpty()) {
                throw new RuntimeException("创建算法任务失败，未返回任务ID");
            }

            // 3. 保存算法任务记录
            List<FicAlgoTaskBO> algoTasks = algoTaskBOList.stream()
                    .map(algoTaskBO -> {
                        FicAlgoTaskBO ficAlgoTaskBO = new FicAlgoTaskBO();
                        ficAlgoTaskBO.setGmtCreate(System.currentTimeMillis());
                        ficAlgoTaskBO.setWorkflowTaskId(workflowTaskId);
                        ficAlgoTaskBO.setStatus(TaskStatusEnum.RUNNING.getCode());
                        ficAlgoTaskBO.setTaskType(algoTaskTypeEnum.name());
                        ficAlgoTaskBO.setAlgoTaskId(algoTaskBO.getAlgoTaskId());
                        ficAlgoTaskBO.setRelevantId(algoTaskBO.getRelevantId());
                        ficAlgoTaskBO.setRelevantIdType(algoTaskBO.getRelevantIdType().getValue());
                        return ficAlgoTaskBO;
                    })
                    .collect(Collectors.toList());
            ficAlgoTaskRepository.saveAll(algoTasks);
            log.info("[runAlgoTask] 保存算法任务记录: {}", algoTasks);

            // 4. 状态检查阶段：检查所有任务状态
            List<FicAlgoTaskBO> ficAlgoTaskBOList = ficAlgoTaskRepository.findByWorkflowTaskIdAndTaskType(workflowTaskId, algoTaskTypeEnum);
            log.info("[runAlgoTask] 查询到算法任务记录: {}", ficAlgoTaskBOList);
            boolean allCompleted = false;
            boolean anyFailed = false;
            int loopCount = 0;
            while (!allCompleted && !anyFailed) {
                allCompleted = true;
                anyFailed = false;
                loopCount++;
                log.info("[runAlgoTask] 状态检查循环第{}次", loopCount);
                for (FicAlgoTaskBO algoTask : ficAlgoTaskBOList) {
                    log.debug("[runAlgoTask] 检查任务状态, algoTaskId: {}, 当前状态: {}", algoTask.getAlgoTaskId(), algoTask.getStatus());
                    if (algoTask.getStatus() == TaskStatusEnum.COMPLETED.getCode()) {
                        continue;
                    }
                    TaskStatusEnum taskStatusEnum = processor.checkSingleTaskStatus(algoTask);
                    log.info("[runAlgoTask] 任务状态, algoTaskId: {}, 检查结果: {}", algoTask.getAlgoTaskId(), taskStatusEnum);
                    if (TaskStatusEnum.FAILED.equals(taskStatusEnum)) {
                        anyFailed = true;
                        break;
                    } else if (TaskStatusEnum.COMPLETED.equals(taskStatusEnum)) {
                        processor.singleTaskSuccessPostProcess(algoTask);
                        algoTask.setStatus(TaskStatusEnum.COMPLETED.getCode());
                        ficAlgoTaskRepository.updateStatus(algoTask.getId(), TaskStatusEnum.COMPLETED);
                        log.info("[runAlgoTask] 任务完成, algoTaskId: {}", algoTask.getAlgoTaskId());
                    } else {
                        allCompleted = false;
                    }
                }
                if (!allCompleted && !anyFailed) {
                    log.info("[runAlgoTask] 任务未全部完成，等待{}毫秒后重试", WAIT_INTERVAL_MILLIS);
                    try {
                        Thread.sleep(WAIT_INTERVAL_MILLIS);
                    } catch (InterruptedException e) {
                        log.warn("[runAlgoTask] 线程被中断");
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            // 5. 后置处理阶段：执行后置处理
            if (allCompleted) {
                log.info("[runAlgoTask] 所有任务已完成，执行后置处理");
                processor.postProcessAllComplete(ficWorkflowTaskBO, algoTasks);
            }
            if (anyFailed) {
                log.error("[runAlgoTask] 存在任务失败，执行失败后置处理");
                processor.postProcessAnyFailed(ficWorkflowTaskBO, algoTasks);
                throw new RuntimeException("运行算法任务失败");
            }
            log.info("[runAlgoTask] 算法任务流程结束, workflowTaskId: {}, algoTaskType: {}", workflowTaskId, algoTaskTypeEnum);
        } catch (Exception e) {
            log.error("[runAlgoTask] 运行算法任务失败, workflowTaskId: {}, algoTaskType: {}", workflowTaskId, algoTaskTypeEnum, e);
            ficWorkflowTaskRepository.updateTaskStatus(workflowTaskId, TaskStatusEnum.FAILED);
            log.warn("[runAlgoTask] 运行算法任务失败, workflowTask置为失败，workflowTaskId: {}", workflowTaskId);
            throw new RuntimeException("运行算法任务失败", e);
        }
    }

    /**
     * 注册任务处理器
     */
    private void registerTaskProcessor(AlgoTaskProcessor processor) {
        taskProcessorMap.put(processor.getAlgoTaskType(), processor);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        for (AlgoTaskProcessor processor : taskProcessors) {
            registerTaskProcessor(processor);
        }
    }
}
