package com.taichu.application.service.inner.algo;

import com.taichu.domain.algo.gateway.AlgoGateway;
import com.taichu.domain.algo.gateway.FileGateway;
import com.taichu.domain.algo.model.AlgoResponse;
import com.taichu.domain.algo.model.common.RoleDTO;
import com.taichu.domain.algo.model.common.UploadFile;
import com.taichu.domain.algo.model.request.ScriptTaskRequest;
import com.taichu.domain.algo.model.response.ScriptResult;
import com.taichu.domain.enums.*;
import com.taichu.domain.model.FicAlgoTaskBO;
import com.taichu.domain.model.FicResourceBO;
import com.taichu.domain.model.FicRoleBO;
import com.taichu.domain.model.FicScriptBO;
import com.taichu.domain.model.FicWorkflowTaskBO;
import com.taichu.infra.repo.FicResourceRepository;
import com.taichu.infra.repo.FicRoleRepository;
import com.taichu.infra.repo.FicScriptRepository;
import com.taichu.infra.repo.FicWorkflowRepository;
import com.taichu.infra.repo.FicWorkflowTaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ScriptGenAlgoTaskProcessor extends AbstractAlgoTaskProcessor {

    private final AlgoGateway algoGateway;
    private final FicScriptRepository ficScriptRepository;
    private final FicWorkflowTaskRepository ficWorkflowTaskRepository;
    private final FicResourceRepository ficResourceRepository;
    private final FicRoleRepository ficRoleRepository;
    private final FileGateway fileGateway;

    public ScriptGenAlgoTaskProcessor(AlgoGateway algoGateway, 
                                    FicScriptRepository ficScriptRepository, 
                                    FicWorkflowTaskRepository ficWorkflowTaskRepository,  
                                    FicWorkflowRepository ficWorkflowRepository,
                                    FicResourceRepository ficResourceRepository,
                                    FicRoleRepository ficRoleRepository,
                                    FileGateway fileGateway) {
        super(ficWorkflowTaskRepository, ficWorkflowRepository);
        this.algoGateway = algoGateway;
        this.ficScriptRepository = ficScriptRepository;
        this.ficWorkflowTaskRepository = ficWorkflowTaskRepository;
        this.ficResourceRepository = ficResourceRepository;
        this.ficRoleRepository = ficRoleRepository;
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
    public List<AlgoTaskBO> generateTasks(FicWorkflowTaskBO workflowTask) {
        Long workflowId = workflowTask.getWorkflowId();
        log.info("[ScriptGenAlgoTaskProcessor.generateTasks] 开始生成剧本任务, workflowId: {}", workflowId);
        try {
            // 获取小说文件
            List<FicResourceBO> novelFiles = ficResourceRepository.findByWorkflowIdAndResourceType(
                    workflowId,
                ResourceTypeEnum.NOVEL_FILE
            );
            log.info("[ScriptGenAlgoTaskProcessor.generateTasks] 查询到小说文件: {}", novelFiles);
            if (novelFiles.isEmpty()) {
                log.error("[ScriptGenAlgoTaskProcessor.generateTasks] 未找到小说文件, workflowId: {}", workflowId);
                return new ArrayList<>();
            }
            
            // 构建剧本生成请求
            ScriptTaskRequest request = new ScriptTaskRequest();
            request.setWorkflowId(String.valueOf(workflowId));
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


            List<String> fileNames = request.getFiles().stream().map(UploadFile::getFileName).collect(Collectors.toList());
            log.info("[ScriptGenAlgoTaskProcessor.generateTasks] workflowId={}, workflowTaskId={}, fileNames: {}", workflowId, workflowTask.getId(), fileNames);
            // 调用算法服务生成剧本
            AlgoResponse response = algoGateway.createScriptTask(request);
            log.info("[ScriptGenAlgoTaskProcessor.generateTasks] 算法服务响应: {}", response);

            // 添加到返回列表
            AlgoTaskBO algoTaskBO = new AlgoTaskBO();
            algoTaskBO.setAlgoTaskId(response.getTaskId());
            algoTaskBO.setRelevantId(workflowId);
            algoTaskBO.setRelevantIdType(RelevanceType.WORKFLOW_ID);

            log.info("[ScriptGenAlgoTaskProcessor.generateTasks] 生成的任务: {}", algoTaskBO);
            // 返回响应列表
            return Collections.singletonList(algoTaskBO);
        } catch (Exception e) {
            log.error("[ScriptGenAlgoTaskProcessor.generateTasks] Failed to generate script task for workflow: " + workflowId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public void singleTaskSuccessPostProcess(FicAlgoTaskBO algoTask) {
        log.info("[ScriptGenAlgoTaskProcessor.singleTaskSuccessPostProcess] 开始处理剧本生成结果, algoTaskId: {}", algoTask.getAlgoTaskId());
        try {
            // 获取生成的剧本内容，添加重试逻辑
            String taskId = Objects.toString(algoTask.getAlgoTaskId());
            ScriptResult result = retryGetResultOperation(
                () -> algoGateway.getScriptResult(taskId),
                "getScriptResult",
                taskId
            );
            
            log.info("[ScriptGenAlgoTaskProcessor.singleTaskSuccessPostProcess] 获取到剧本结果: {}", result);
            if (result == null || result.getScripts() == null || result.getScripts().isEmpty()) {
                log.error("[ScriptGenAlgoTaskProcessor.singleTaskSuccessPostProcess] 获取剧本结果失败或结果为空, algoTaskId: {}", algoTask.getAlgoTaskId());
                return;
            }
            
            // 根据 workflowTaskId 查询工作流任务，获取 workflowId
            FicWorkflowTaskBO workflowTask = ficWorkflowTaskRepository.findById(algoTask.getWorkflowTaskId());
            if (workflowTask == null) {
                log.error("[ScriptGenAlgoTaskProcessor.singleTaskSuccessPostProcess] 工作流任务不存在, workflowTaskId: {}", algoTask.getWorkflowTaskId());
                return;
            }

            log.info("[ScriptGenAlgoTaskProcessor.singleTaskSuccessPostProcess] 下线旧剧本，workflowId: {}", workflowTask.getWorkflowId());
            // 下线workflow所有状态为VALID的 script
            ficScriptRepository.offlineByWorkflowId(workflowTask.getWorkflowId());
            
            // 保存每个剧本片段
            for (int i = 0; i < result.getScripts().size(); i++) {
                FicScriptBO scriptBO = new FicScriptBO();
                scriptBO.setGmtCreate(System.currentTimeMillis());
                scriptBO.setWorkflowId(workflowTask.getWorkflowId());
                scriptBO.setContent(result.getScripts().get(i));
                scriptBO.setOrderIndex((long) (i + 1));
                scriptBO.setStatus(CommonStatusEnum.VALID.getValue());
                ficScriptRepository.insert(scriptBO);
                log.info("[ScriptGenAlgoTaskProcessor.singleTaskSuccessPostProcess] 保存剧本片段, orderIndex: {}", scriptBO.getOrderIndex());
            }

            // 保存每个角色
            if (result.getRoles() != null && !result.getRoles().isEmpty()) {
                log.info("[ScriptGenAlgoTaskProcessor.singleTaskSuccessPostProcess] 下线旧角色，workflowId: {}", workflowTask.getWorkflowId());
                // 下线workflow所有状态为VALID的角色
                ficRoleRepository.offlineByWorkflowId(workflowTask.getWorkflowId());
                
                // 保存新的角色信息
                for (RoleDTO roleDTO : result.getRoles()) {
                    FicRoleBO roleBO = new FicRoleBO();
                    roleBO.setGmtCreate(System.currentTimeMillis());
                    roleBO.setWorkflowId(workflowTask.getWorkflowId());
                    roleBO.setRoleName(roleDTO.getRole());
                    roleBO.setDescription(roleDTO.getDescription());
                    roleBO.setPrompt(roleDTO.getPrompt());
                    roleBO.setStatus(CommonStatusEnum.VALID.getValue());
                    roleBO.setGmtCreate(System.currentTimeMillis());
                    ficRoleRepository.insert(roleBO);
                    log.info("[ScriptGenAlgoTaskProcessor.singleTaskSuccessPostProcess] 保存角色, name: {}", roleDTO.getRole());
                }
            }
            
            log.info("[ScriptGenAlgoTaskProcessor.singleTaskSuccessPostProcess] Script generation completed for task: {}", algoTask.getAlgoTaskId());
        } catch (Exception e) {
            log.error("[ScriptGenAlgoTaskProcessor.singleTaskSuccessPostProcess] Failed to process script generation result for task: " + algoTask.getAlgoTaskId(), e);
        }
    }
}
