package com.taichu.application.service.inner;

import com.taichu.domain.algo.gateway.AlgoGateway;
import com.taichu.domain.algo.model.AlgoResponse;
import com.taichu.domain.algo.model.request.StoryboardImageRequest;
import com.taichu.domain.algo.model.request.StoryboardTextRequest;
import com.taichu.domain.enums.AlgoTaskTypeEnum;
import com.taichu.domain.enums.TaskStatusEnum;
import com.taichu.domain.model.FicAlgoTaskBO;
import com.taichu.domain.model.FicWorkflowTaskBO;
import com.taichu.domain.model.TaskStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class StoryboardTaskProcessor implements AlgoTaskInnerService.AlgoTaskProcessor {

    private final AlgoGateway algoGateway;

    public StoryboardTaskProcessor(AlgoGateway algoGateway) {
        this.algoGateway = algoGateway;
    }

    @Override
    public List<AlgoResponse> generateTasks(FicWorkflowTaskBO workflowTask) {
        List<AlgoResponse> responses = new ArrayList<>();
        
        // 1. 创建分镜文本任务
        StoryboardTextRequest textRequest = new StoryboardTextRequest();
        textRequest.setWorkflowId(String.valueOf(workflowTask.getWorkflowId()));
        AlgoResponse textResponse = algoGateway.createStoryboardTextTask(textRequest);
        if (textResponse != null && textResponse.isSuccess()) {
            responses.add(textResponse);
        }

        // 2. 创建分镜图片任务
        StoryboardImageRequest imageRequest = new StoryboardImageRequest();
        imageRequest.setWorkflowId(String.valueOf(workflowTask.getWorkflowId()));
        AlgoResponse imageResponse = algoGateway.createStoryboardImageTask(imageRequest);
        if (imageResponse != null && imageResponse.isSuccess()) {
            responses.add(imageResponse);
        }

        return responses;
    }

    @Override
    public boolean checkTaskStatus(FicAlgoTaskBO algoTask) {
        TaskStatus status = algoGateway.checkTaskStatus(Objects.toString(algoTask.getAlgoTaskId()));
        if (status == null) {
            log.error("检查任务[{}]状态失败", algoTask.getAlgoTaskId());
            algoTask.setStatus(TaskStatusEnum.FAILED.getCode());
            return false;
        }

        if (status.isCompleted()) {
            algoTask.setStatus(TaskStatusEnum.COMPLETED.getCode());
            return true;
        } else if (status.isFailed()) {
            log.error("任务[{}]执行失败", algoTask.getAlgoTaskId());
            algoTask.setStatus(TaskStatusEnum.FAILED.getCode());
            return false;
        }

        return true;
    }

    @Override
    public void postProcess(FicWorkflowTaskBO workflowTask, List<FicAlgoTaskBO> algoTasks) {

        // TODO@chai 补充逻辑
//        // 1. 获取分镜文本任务结果
//        FicAlgoTaskBO textTask = algoTasks.stream()
//            .filter(task -> task.getTaskType().equals(AlgoTaskTypeEnum.STORYBOARD_TEXT.getCode()))
//            .findFirst()
//            .orElse(null);
//
//        // 2. 获取分镜图片任务结果
//        FicAlgoTaskBO imageTask = algoTasks.stream()
//            .filter(task -> task.getTaskType().equals(AlgoTaskTypeEnum.STORYBOARD_IMAGE.getCode()))
//            .findFirst()
//            .orElse(null);
//
//        // 3. 合并分镜结果
//        if (textTask != null && imageTask != null) {
//            // TODO: 实现分镜结果合并逻辑
//            log.info("合并分镜结果，工作流任务ID: {}", workflowTask.getId());
//        }
    }
} 