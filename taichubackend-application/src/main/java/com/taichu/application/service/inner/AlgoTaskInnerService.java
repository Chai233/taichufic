package com.taichu.application.service.inner;

import com.google.common.primitives.Longs;
import com.taichu.domain.algo.gateway.AlgoGateway;
import com.taichu.domain.algo.model.AlgoResponse;
import com.taichu.domain.enums.AlgoTaskTypeEnum;
import com.taichu.domain.enums.TaskStatusEnum;
import com.taichu.domain.model.FicAlgoTaskBO;
import com.taichu.domain.model.FicWorkflowTaskBO;
import com.taichu.domain.model.TaskStatus;
import com.taichu.infra.repo.FicAlgoTaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AlgoTaskInnerService {

    public static final int WAIT_INTERVAL_MILLIS = 5000;
    private final AlgoGateway algoGateway;
    private final FicAlgoTaskRepository ficAlgoTaskRepository;
    private final Map<AlgoTaskTypeEnum, AlgoTaskProcessor> taskProcessors = new ConcurrentHashMap<>();

    public AlgoTaskInnerService(AlgoGateway algoGateway, FicAlgoTaskRepository ficAlgoTaskRepository) {
        this.algoGateway = algoGateway;
        this.ficAlgoTaskRepository = ficAlgoTaskRepository;
    }

    /**
     * 算法任务处理器接口
     */
    public interface AlgoTaskProcessor {
        /**
         * 生成阶段：创建算法任务
         */
        List<AlgoResponse> generateTasks(FicWorkflowTaskBO workflowTask);

        /**
         * 状态检查阶段：检查任务状态
         */
        boolean checkTaskStatus(FicAlgoTaskBO algoTask);

        /**
         * 后置处理阶段：处理任务完成后的业务逻辑
         */
        void postProcess(FicWorkflowTaskBO workflowTask, List<FicAlgoTaskBO> algoTasks);
    }

    /**
     * 注册任务处理器
     */
    public void registerTaskProcessor(AlgoTaskTypeEnum taskType, AlgoTaskProcessor processor) {
        taskProcessors.put(taskType, processor);
    }

    /**
     * 运行算法任务
     */
    public void runAlgoTask(FicWorkflowTaskBO ficWorkflowTaskBO, AlgoTaskTypeEnum algoTaskTypeEnum) {
        try {
            // 1. 获取任务处理器
            AlgoTaskProcessor processor = taskProcessors.get(algoTaskTypeEnum);
            if (processor == null) {
                log.error("未找到任务类型[{}]的处理器", algoTaskTypeEnum);
                return;
            }

            // 2. 生成阶段：创建算法任务
            List<AlgoResponse> responses = processor.generateTasks(ficWorkflowTaskBO);
            if (responses == null || responses.isEmpty()) {
                log.error("创建算法任务失败，未返回任务ID");
                return;
            }

            // 3. 保存算法任务记录
            List<FicAlgoTaskBO> algoTasks = responses.stream()
                .map(response -> {
                    FicAlgoTaskBO algoTask = new FicAlgoTaskBO();
                    algoTask.setWorkflowTaskId(ficWorkflowTaskBO.getId());
                    algoTask.setStatus(TaskStatusEnum.RUNNING.getCode());
                    algoTask.setTaskType(algoTaskTypeEnum.name());
                    algoTask.setAlgoTaskId(Longs.tryParse(response.getTaskId()));
                    return algoTask;
                })
                        .collect(Collectors.toList());

            ficAlgoTaskRepository.saveAll(algoTasks);

            // 4. 状态检查阶段：检查所有任务状态
            boolean allCompleted = false;
            boolean anyFailed = false;

            while (!allCompleted && !anyFailed) {
                allCompleted = true;
                anyFailed = false;

                for (FicAlgoTaskBO algoTask : algoTasks) {
                    if (!processor.checkTaskStatus(algoTask)) {
                        anyFailed = true;
                        break;
                    }

                    if (algoTask.getStatus().equals(TaskStatusEnum.COMPLETED.getCode())) {
                        ficAlgoTaskRepository.save(algoTask);
                    } else if (algoTask.getStatus().equals(TaskStatusEnum.FAILED.getCode())) {
                        anyFailed = true;
                        break;
                    } else {
                        allCompleted = false;
                    }
                }

                if (!allCompleted && !anyFailed) {
                    try {
                        Thread.sleep(WAIT_INTERVAL_MILLIS); // 等待1秒后再次检查
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            // 5. 后置处理阶段：执行后置处理
            if (allCompleted) {
                processor.postProcess(ficWorkflowTaskBO, algoTasks);
            }

        } catch (Exception e) {
            log.error("运行算法任务失败", e);
            throw new RuntimeException("运行算法任务失败", e);
        }
    }
}
