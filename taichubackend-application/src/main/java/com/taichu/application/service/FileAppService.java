package com.taichu.application.service;

import com.alibaba.cola.dto.SingleResponse;
import com.taichu.application.annotation.EntranceLog;
import com.taichu.application.helper.WorkflowValidationHelper;
import com.taichu.common.common.exception.AppServiceExceptionHandle;
import com.taichu.domain.algo.gateway.FileGateway;
import com.taichu.domain.enums.*;
import com.taichu.domain.model.FicResourceBO;
import com.taichu.infra.repo.FicResourceRepository;
import com.taichu.infra.repo.FicWorkflowRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Component
public class FileAppService {

    @Autowired
    private FileGateway fileGateway;

    @Autowired
    private WorkflowValidationHelper workflowValidationHelper;

    @Autowired
    private FicResourceRepository ficResourceRepository;
    @Autowired
    private FicWorkflowRepository ficWorkflowRepository;

    @EntranceLog(bizCode = "UPLOAD_FILES")
    @AppServiceExceptionHandle(biz = "UPLOAD_FILES")
    @Transactional
    public SingleResponse<?> uploadFiles(List<MultipartFile> files, Long workflowId, Long userId) {
        try {
            // 校验工作流
            SingleResponse<?> validateResponse = workflowValidationHelper.validateWorkflow(workflowId, userId, WorkflowStatusEnum.INIT_WAIT_FOR_FILE);
            if (!validateResponse.isSuccess()) {
                return validateResponse;
            }

            // 存储文件并创建资源记录
            for (MultipartFile file : files) {
                // 保存文件到存储系统
                String fileObjName = fileGateway.saveFile(file.getOriginalFilename(), file).getData();
                if (fileObjName == null) {
                    log.error("Failed to save file: {}", file.getOriginalFilename());
                    return SingleResponse.buildFailure("FILE_SAVE_ERROR", "文件保存失败");
                }

                // 创建资源记录
                FicResourceBO resource = new FicResourceBO();
                resource.setWorkflowId(workflowId);
                resource.setStatus(CommonStatusEnum.VALID.getValue());
                resource.setResourceType(ResourceTypeEnum.NOVEL_FILE.name());
                resource.setResourceStorageType(ResourceStorageTypeEnum.ALI_CLOUD_OSS.name());
                resource.setResourceUrl(fileObjName);
                resource.setOriginName(file.getOriginalFilename());
                resource.setRelevanceId(workflowId);
                resource.setRelevanceType(RelevanceType.WORKFLOW_ID.getValue());
                resource.setGmtCreate(System.currentTimeMillis());

                ficResourceRepository.insert(resource);
            }

            // 更新workflow状态
            ficWorkflowRepository.updateStatus(workflowId, WorkflowStatusEnum.UPLOAD_FILE_DONE.getCode());

            return SingleResponse.buildSuccess();
        } catch (Exception e) {
            log.error("Failed to upload files for workflow: {}, error: ", workflowId, e);
            return SingleResponse.buildFailure("FILE_UPLOAD_ERROR", "文件上传失败");
        }
    }
}
