package com.taichu.application.service.inner.algo.v2;

import com.taichu.application.service.inner.algo.v2.context.AlgoTaskContext;
import com.taichu.application.service.inner.algo.v2.context.ScriptGenTaskContext;
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
import com.taichu.infra.repo.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ScriptGenAlgoTaskProcessorV2 extends AbstractAlgoTaskProcessorV2 {

    private final AlgoGateway algoGateway;
    private final FicScriptRepository ficScriptRepository;
    private final FicResourceRepository ficResourceRepository;
    private final FicRoleRepository ficRoleRepository;
    private final FileGateway fileGateway;

    public ScriptGenAlgoTaskProcessorV2(AlgoGateway algoGateway, 
                                    FicScriptRepository ficScriptRepository, 
                                    FicWorkflowTaskRepository ficWorkflowTaskRepository,
                                    FicWorkflowRepository ficWorkflowRepository,
                                    FicResourceRepository ficResourceRepository,
                                    FicRoleRepository ficRoleRepository,
                                    FileGateway fileGateway) {
        super(ficWorkflowTaskRepository, ficWorkflowRepository);
        this.algoGateway = algoGateway;
        this.ficScriptRepository = ficScriptRepository;
        this.ficResourceRepository = ficResourceRepository;
        this.ficRoleRepository = ficRoleRepository;
        this.fileGateway = fileGateway;
    }

    @Override
    public AlgoTaskTypeEnum getAlgoTaskType() {
        return AlgoTaskTypeEnum.SCRIPT_GENERATION;
    }

    @Override
    public List<AlgoTaskContext> createTaskContextList(FicWorkflowTaskBO workflowTask) {
        Long workflowId = workflowTask.getWorkflowId();
        log.info("[ScriptGenAlgoTaskProcessorV2.createTaskContextList] 开始创建剧本生成任务上下文, workflowId: {}", workflowId);
        
        // 获取小说文件
        List<FicResourceBO> novelFiles = ficResourceRepository.findValidByWorkflowIdAndResourceType(
            workflowId, ResourceTypeEnum.NOVEL_FILE
        );
        
        if (novelFiles.isEmpty()) {
            log.error("[ScriptGenAlgoTaskProcessorV2.createTaskContextList] 未找到小说文件, workflowId: {}", workflowId);
            return List.of();
        }
        
        // 创建单个任务上下文（剧本生成通常只有一个任务）
        ScriptGenTaskContext context = new ScriptGenTaskContext();
        context.setWorkflowId(workflowId);
        context.setWorkflowTaskId(workflowTask.getId());
        context.setNovelFiles(novelFiles);
        context.setUserPrompt(workflowTask.getParams().get(WorkflowTaskConstant.SCRIPT_PROMPT));
        
        return List.of(context);
    }

    @Override
    public void validateContext(AlgoTaskContext context) {
        ScriptGenTaskContext scriptContext = (ScriptGenTaskContext) context;
        if (scriptContext.getNovelFiles() == null || scriptContext.getNovelFiles().isEmpty()) {
            throw new IllegalArgumentException("小说文件列表不能为空");
        }
        if (scriptContext.getWorkflowId() == null) {
            throw new IllegalArgumentException("工作流ID不能为空");
        }
    }

    @Override
    public AlgoTaskBOV2 generateAlgoTask(AlgoTaskContext context) {
        ScriptGenTaskContext scriptContext = (ScriptGenTaskContext) context;
        
        // 构建剧本生成请求
        ScriptTaskRequest request = new ScriptTaskRequest();
        request.setWorkflowId(String.valueOf(scriptContext.getWorkflowId()));
        request.setPrompt(scriptContext.getUserPrompt());
        
        // 设置小说文件
        List<UploadFile> files = scriptContext.getNovelFiles().stream()
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
        
        // 调用算法服务（使用基类的重试机制）
        String operationName = "Create script task for workflow: " + scriptContext.getWorkflowId();
        AlgoResponse response = callAlgoServiceWithRetry(operationName, () -> algoGateway.createScriptTask(request));
        
        if (response == null) {
            throw new RuntimeException("创建剧本生成任务失败");
        }
        
        // 创建AlgoTaskBO
        AlgoTaskBOV2 algoTaskBO = new AlgoTaskBOV2();
        algoTaskBO.setAlgoTaskId(response.getTaskId());
        algoTaskBO.setRelevantId(scriptContext.getWorkflowId());
        algoTaskBO.setRelevantIdType(RelevanceType.WORKFLOW_ID);
        algoTaskBO.setTaskSummary(scriptContext.getTaskSummary());
        
        return algoTaskBO;
    }

    @Override
    public void singleTaskSuccessPostProcess(FicAlgoTaskBO algoTask, AlgoTaskContext context) throws Exception {
        ScriptGenTaskContext scriptContext = (ScriptGenTaskContext) context;
        log.info("[ScriptGenAlgoTaskProcessorV2.singleTaskSuccessPostProcess] 开始处理剧本生成结果, algoTaskId: {}", algoTask.getAlgoTaskId());
        
        // 获取生成的剧本内容（使用基类的重试机制）
        String taskId = Objects.toString(algoTask.getAlgoTaskId());
        ScriptResult result = retryGetResultOperation(
            () -> algoGateway.getScriptResult(taskId),
            "getScriptResult",
            taskId
        );
        
        if (result == null || result.getScripts() == null || result.getScripts().isEmpty()) {
            throw new Exception("获取剧本结果失败或结果为空");
        }
        
        // 处理剧本和角色数据
        processScriptResult(result, scriptContext);
        
        log.info("[ScriptGenAlgoTaskProcessorV2.singleTaskSuccessPostProcess] 剧本生成任务完成: {}", scriptContext.getTaskSummary());
    }

    @Override
    public void singleTaskFailedPostProcess(FicAlgoTaskBO algoTask, AlgoTaskContext context, Exception e) {
        log.error("[ScriptGenAlgoTaskProcessorV2.singleTaskFailedPostProcess] 剧本生成任务失败: {}", 
            algoTask.buildSummary(), e);
    }

    @Override
    public void postProcessAllComplete(FicWorkflowTaskBO workflowTask, List<AlgoTaskContext> contexts) {
        log.info("[ScriptGenAlgoTaskProcessorV2.postProcessAllComplete] 所有剧本生成任务完成, workflowId: {}", workflowTask.getWorkflowId());
    }

    @Override
    public void postProcessAnyFailed(FicWorkflowTaskBO workflowTask, List<AlgoTaskContext> contexts) {
        super.postProcessAnyFailed(workflowTask, contexts);
        Long workflowId = workflowTask.getWorkflowId();
        cleanupFailedScriptTask(workflowId);
    }

    private void cleanupFailedScriptTask(Long workflowId) {
        // 清理已创建的剧本和角色记录
        try {
            ficScriptRepository.offlineByWorkflowId(workflowId);
            ficRoleRepository.offlineByWorkflowId(workflowId);
            log.info("[cleanupFailedScriptTask] 清理失败任务资源成功, workflowId: {}", workflowId);
        } catch (Exception e) {
            log.error("[cleanupFailedScriptTask] 清理失败任务资源失败, workflowId: {}", workflowId, e);
        }
    }

    private void processScriptResult(ScriptResult result, ScriptGenTaskContext context) {
        // 下线旧剧本
        ficScriptRepository.offlineByWorkflowId(context.getWorkflowId());
        
        // 保存每个剧本片段
        for (int i = 0; i < result.getScripts().size(); i++) {
            FicScriptBO scriptBO = new FicScriptBO();
            scriptBO.setGmtCreate(System.currentTimeMillis());
            scriptBO.setWorkflowId(context.getWorkflowId());
            scriptBO.setContent(result.getScripts().get(i));
            scriptBO.setOrderIndex((long) (i + 1));
            scriptBO.setStatus(CommonStatusEnum.VALID.getValue());
            ficScriptRepository.insert(scriptBO);
            log.info("[processScriptResult] 保存剧本片段, orderIndex: {}", scriptBO.getOrderIndex());
        }

        // 保存每个角色
        if (result.getRoles() != null && !result.getRoles().isEmpty()) {
            // 下线旧角色
            ficRoleRepository.offlineByWorkflowId(context.getWorkflowId());
            
            // 保存新的角色信息
            for (RoleDTO roleDTO : result.getRoles()) {
                FicRoleBO roleBO = new FicRoleBO();
                roleBO.setGmtCreate(System.currentTimeMillis());
                roleBO.setWorkflowId(context.getWorkflowId());
                roleBO.setRoleName(roleDTO.getRole());
                roleBO.setDescription(roleDTO.getDescription());
                roleBO.setPrompt(roleDTO.getPrompt());
                roleBO.setStatus(CommonStatusEnum.VALID.getValue());
                ficRoleRepository.insert(roleBO);
                log.info("[processScriptResult] 保存角色, name: {}", roleDTO.getRole());
            }
        }
    }

    @Override
    protected AlgoGateway getAlgoGateway() {
        return algoGateway;
    }

    @Override
    protected Logger getLogger() {
        return log;
    }
} 