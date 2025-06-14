package com.taichu.application.service.inner.algo;

import com.taichu.domain.algo.gateway.AlgoGateway;
import com.taichu.domain.algo.model.AlgoResponse;
import com.taichu.domain.algo.model.request.ScriptTaskRequest;
import com.taichu.domain.algo.model.response.ScriptResult;
import com.taichu.domain.enums.AlgoTaskTypeEnum;
import com.taichu.domain.enums.CommonStatusEnum;
import com.taichu.domain.enums.TaskStatusEnum;
import com.taichu.domain.enums.WorkflowStatusEnum;
import com.taichu.domain.model.FicAlgoTaskBO;
import com.taichu.domain.model.FicScriptBO;
import com.taichu.domain.model.FicWorkflowTaskBO;
import com.taichu.infra.repo.FicScriptRepository;
import com.taichu.infra.repo.FicWorkflowRepository;
import com.taichu.infra.repo.FicWorkflowTaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@Slf4j
public class ScriptGenAlgoTaskProcessor extends AbstractAlgoTaskProcessor {

    private final AlgoGateway algoGateway;
    private final FicScriptRepository ficScriptRepository;
    private final FicWorkflowTaskRepository ficWorkflowTaskRepository;

    @Autowired
    public ScriptGenAlgoTaskProcessor(AlgoGateway algoGateway, FicScriptRepository ficScriptRepository, FicWorkflowTaskRepository ficWorkflowTaskRepository,  FicWorkflowRepository ficWorkflowRepository) {
        super(ficWorkflowTaskRepository, ficWorkflowRepository);
        this.algoGateway = algoGateway;
        this.ficScriptRepository = ficScriptRepository;
        this.ficWorkflowTaskRepository = ficWorkflowTaskRepository;
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    @Override
    protected AlgoGateway getAlgoGateway() {
        return algoGateway;
    }

    @Override
    public AlgoTaskTypeEnum getAlgoTaskType() {
        return AlgoTaskTypeEnum.SCRIPT_GENERATION;
    }

    @Override
    public List<AlgoResponse> generateTasks(FicWorkflowTaskBO workflowTask) {
        try {
            // 构建剧本生成请求
            ScriptTaskRequest request = new ScriptTaskRequest();
            request.setWorkflowId(String.valueOf(workflowTask.getWorkflowId()));
            
            // 调用算法服务生成剧本
            AlgoResponse response = algoGateway.createScriptTask(request);
            
            // 返回响应列表
            List<AlgoResponse> responses = new ArrayList<>();
            responses.add(response);
            return responses;
        } catch (Exception e) {
            log.error("Failed to generate script task for workflow: " + workflowTask.getWorkflowId(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public void singleTaskSuccessPostProcess(FicAlgoTaskBO algoTask) {
        try {
            // 获取生成的剧本内容
            ScriptResult result = algoGateway.getScriptResult(Objects.toString(algoTask.getAlgoTaskId()));
            if (result == null || result.getScripts() == null || result.getScripts().isEmpty()) {
                log.error("获取剧本结果失败或结果为空, algoTaskId: {}", algoTask.getAlgoTaskId());
                return;
            }
            
            // 将所有剧本片段合并为一个字符串
            String scriptContent = String.join("\n", result.getScripts());
            
            // 更新算法任务结果
            algoTask.setTaskAbstract(scriptContent);
            
            // 根据 workflowTaskId 查询工作流任务，获取 workflowId
            FicWorkflowTaskBO workflowTask = ficWorkflowTaskRepository.findById(algoTask.getWorkflowTaskId());
            if (workflowTask == null) {
                log.error("工作流任务不存在, workflowTaskId: {}", algoTask.getWorkflowTaskId());
                return;
            }
            
            // 保存每个剧本片段
            for (int i = 0; i < result.getScripts().size(); i++) {
                FicScriptBO scriptBO = new FicScriptBO();
                scriptBO.setWorkflowId(workflowTask.getWorkflowId());
                scriptBO.setContent(result.getScripts().get(i));
                scriptBO.setOrderIndex((long) (i + 1));
                scriptBO.setStatus(CommonStatusEnum.VALID.getValue());
                ficScriptRepository.insert(scriptBO);
            }
            
            log.info("Script generation completed for task: " + algoTask.getAlgoTaskId());
        } catch (Exception e) {
            log.error("Failed to process script generation result for task: " + algoTask.getAlgoTaskId(), e);
        }
    }
}
