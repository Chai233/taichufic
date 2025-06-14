package com.taichu.application.service.inner.algo;

import com.google.common.primitives.Longs;
import com.taichu.domain.algo.gateway.AlgoGateway;
import com.taichu.domain.algo.model.AlgoResponse;
import com.taichu.domain.enums.AlgoTaskTypeEnum;
import com.taichu.domain.enums.TaskStatusEnum;
import com.taichu.domain.model.FicAlgoTaskBO;
import com.taichu.domain.model.FicWorkflowTaskBO;
import com.taichu.infra.repo.FicAlgoTaskRepository;
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

    public static final int WAIT_INTERVAL_MILLIS = 5000;
    private final AlgoGateway algoGateway;
    private final FicAlgoTaskRepository ficAlgoTaskRepository;
    private final List<AlgoTaskProcessor> taskProcessors = new ArrayList<>();
    private final Map<AlgoTaskTypeEnum, AlgoTaskProcessor> taskProcessorMap = new ConcurrentHashMap<>();

    public AlgoTaskInnerService(AlgoGateway algoGateway, FicAlgoTaskRepository ficAlgoTaskRepository, List<AlgoTaskProcessor> algoTaskProcessors) {
        this.algoGateway = algoGateway;
        this.ficAlgoTaskRepository = ficAlgoTaskRepository;
        this.taskProcessors.addAll(algoTaskProcessors);
    }

    /**
     * 运行算法任务
     */
    public void runAlgoTask(FicWorkflowTaskBO ficWorkflowTaskBO, AlgoTaskTypeEnum algoTaskTypeEnum) {
        try {
            Long workflowTaskId = ficWorkflowTaskBO.getId();

            // 1. 获取任务处理器
            AlgoTaskProcessor processor = taskProcessorMap.get(algoTaskTypeEnum);
            if (processor == null) {
                log.error("未找到任务类型[{}]的处理器", algoTaskTypeEnum);
                return;
            }

            // 2. 生成阶段：创建算法任务
            List<AlgoTaskBO> algoTaskBOList = processor.generateTasks(ficWorkflowTaskBO);
            if (algoTaskBOList == null || algoTaskBOList.isEmpty()) {
                log.error("创建算法任务失败，未返回任务ID");
                return;
            }

            // 3. 保存算法任务记录
            List<FicAlgoTaskBO> algoTasks = algoTaskBOList.stream()
                    .map(algoTaskBO -> {
                        FicAlgoTaskBO algoTask = new FicAlgoTaskBO();
                        algoTask.setWorkflowTaskId(workflowTaskId);
                        algoTask.setStatus(TaskStatusEnum.RUNNING.getCode());
                        algoTask.setTaskType(algoTaskTypeEnum.name());
                        algoTask.setAlgoTaskId(Longs.tryParse(algoTaskBO.getAlgoTaskId()));
                        algoTask.setRelevantId(algoTaskBO.getRelevantId());
                        algoTask.setRelevantIdType(algoTaskBO.getRelevantIdType().getValue());
                        return algoTask;
                    })
                    .collect(Collectors.toList());
            ficAlgoTaskRepository.saveAll(algoTasks);


            // 4. 状态检查阶段：检查所有任务状态
            List<FicAlgoTaskBO> ficAlgoTaskBOList = ficAlgoTaskRepository.findByWorkflowTaskIdAndTaskType(workflowTaskId, algoTaskTypeEnum);
            boolean allCompleted = false;
            boolean anyFailed = false;
            while (!allCompleted && !anyFailed) {
                allCompleted = true;
                anyFailed = false;

                for (FicAlgoTaskBO algoTask : ficAlgoTaskBOList) {
                    if (algoTask.getStatus() == TaskStatusEnum.COMPLETED.getCode()) {
                        continue;
                    }

                    TaskStatusEnum taskStatusEnum = processor.checkSingleTaskStatus(algoTask);
                    if (TaskStatusEnum.FAILED.equals(taskStatusEnum)) {
                        anyFailed = true;
                        break;
                    } else if (TaskStatusEnum.COMPLETED.equals(taskStatusEnum)) {
                        processor.singleTaskSuccessPostProcess(algoTask);
                        algoTask.setStatus(TaskStatusEnum.COMPLETED.getCode());
                        ficAlgoTaskRepository.updateStatus(algoTask.getId(), TaskStatusEnum.COMPLETED);
                    } else {
                        allCompleted = false;
                    }
                }

                if (!allCompleted && !anyFailed) {
                    try {
                        Thread.sleep(WAIT_INTERVAL_MILLIS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            // 5. 后置处理阶段：执行后置处理
            if (allCompleted) {
                processor.postProcessAllComplete(ficWorkflowTaskBO, algoTasks);
            }
            if (anyFailed) {
                processor.postProcessAnyFailed(ficWorkflowTaskBO, algoTasks);
            }

        } catch (Exception e) {
            log.error("运行算法任务失败", e);
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
