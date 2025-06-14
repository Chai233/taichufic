package com.taichu.application.service.inner.algo;

import com.taichu.domain.algo.gateway.AlgoGateway;
import com.taichu.domain.algo.gateway.FileGateway;
import com.taichu.domain.algo.model.AlgoResponse;
import com.taichu.domain.algo.model.common.UploadFile;
import com.taichu.domain.algo.model.request.ScriptTaskRequest;
import com.taichu.domain.algo.model.response.ScriptResult;
import com.taichu.domain.enums.AlgoTaskTypeEnum;
import com.taichu.domain.enums.CommonStatusEnum;
import com.taichu.domain.enums.ResourceTypeEnum;
import com.taichu.domain.enums.WorkflowTaskConstant;
import com.taichu.domain.model.FicAlgoTaskBO;
import com.taichu.domain.model.FicResourceBO;
import com.taichu.domain.model.FicScriptBO;
import com.taichu.domain.model.FicWorkflowTaskBO;
import com.taichu.infra.repo.FicResourceRepository;
import com.taichu.infra.repo.FicScriptRepository;
import com.taichu.infra.repo.FicWorkflowRepository;
import com.taichu.infra.repo.FicWorkflowTaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ScriptGenAlgoTaskProcessor extends AbstractAlgoTaskProcessor {

    private final AlgoGateway algoGateway;
    private final FicScriptRepository ficScriptRepository;
    private final FicWorkflowTaskRepository ficWorkflowTaskRepository;
    private final FicResourceRepository ficResourceRepository;
    private final FileGateway fileGateway;

    public ScriptGenAlgoTaskProcessor(AlgoGateway algoGateway, 
                                    FicScriptRepository ficScriptRepository, 
                                    FicWorkflowTaskRepository ficWorkflowTaskRepository,  
                                    FicWorkflowRepository ficWorkflowRepository,
                                    FicResourceRepository ficResourceRepository,
                                    FileGateway fileGateway) {
        super(ficWorkflowTaskRepository, ficWorkflowRepository);
        this.algoGateway = algoGateway;
        this.ficScriptRepository = ficScriptRepository;
        this.ficWorkflowTaskRepository = ficWorkflowTaskRepository;
        this.ficResourceRepository = ficResourceRepository;
        this.fileGateway = fileGateway;
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
            // 获取小说文件
            List<FicResourceBO> novelFiles = ficResourceRepository.findByWorkflowIdAndResourceType(
                workflowTask.getWorkflowId(), 
                ResourceTypeEnum.NOVEL_FILE
            );
            
            if (novelFiles.isEmpty()) {
                log.error("未找到小说文件, workflowId: {}", workflowTask.getWorkflowId());
                return new ArrayList<>();
            }
            
            // 构建剧本生成请求
            ScriptTaskRequest request = new ScriptTaskRequest();
            request.setWorkflowId(String.valueOf(workflowTask.getWorkflowId()));
            Optional.ofNullable(workflowTask.getParams().get(WorkflowTaskConstant.SCRIPT_PROMPT)).ifPresentOrElse(request::setPrompt, () -> request.setPrompt(""));
            
            // 设置小说文件，支持 txt 和 pdf
            List<UploadFile> files = novelFiles.stream()
                .map(novelFile -> {
                    UploadFile uploadFile = new UploadFile();
                    uploadFile.setFileName(novelFile.getOriginName());
                    uploadFile.setFileContent(fileGateway.getFileObj(novelFile.getResourceUrl()));
                    String lowerName = novelFile.getOriginName() != null ? novelFile.getOriginName().toLowerCase() : "";
                    if (lowerName.endsWith(".pdf")) {
                        uploadFile.setContentType("application/pdf");
                    } else {
                        uploadFile.setContentType("text/plain");
                    }
                    return uploadFile;
                })
                .collect(Collectors.toList());
            request.setFiles(files);
            
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
