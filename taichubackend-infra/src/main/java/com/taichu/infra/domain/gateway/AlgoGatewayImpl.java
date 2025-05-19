package com.taichu.infra.domain.gateway;

import com.taichu.domain.gateway.AlgoGateway;
import com.taichu.domain.model.AlgoResponse;
import com.taichu.domain.model.AlgoResult;
import com.taichu.domain.model.TaskStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AlgoGatewayImpl implements AlgoGateway {

    @Override
    public AlgoResponse submitScriptTask(Long workflowId) {
        // TODO: 调用算法服务
        AlgoResponse response = new AlgoResponse();
        response.setTaskId(1L);
        return response;
    }

    @Override
    public AlgoResponse submitStoryboardTask(Long workflowId) {
        // TODO: 调用算法服务
        AlgoResponse response = new AlgoResponse();
        response.setTaskId(1L);
        return response;
    }

    @Override
    public TaskStatus checkTaskStatus(Long taskId) {
        // TODO: 调用算法服务
        TaskStatus status = new TaskStatus();
        status.setCode((byte) 1);
        return status;
    }

    @Override
    public AlgoResult getTaskResult(Long workflowId) {
        // TODO: 调用算法服务
        AlgoResult result = new AlgoResult();
        result.setScriptContent("剧本内容");
        result.setStoryboardContent("分镜内容");
        return result;
    }
} 