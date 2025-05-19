package com.taichu.application.service;

import com.alibaba.cola.dto.SingleResponse;
import com.taichu.application.helper.WorkflowValidationHelper;
import com.taichu.domain.enums.WorkflowStatusEnum;
import com.taichu.domain.service.FileDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Component
public class FileAppService {

    @Autowired
    private FileDomainService fileDomainService;

    @Autowired
    private WorkflowValidationHelper workflowValidationHelper;

    public SingleResponse<?> uploadFiles(List<MultipartFile> files, Long workflowId, Long userId) {
        try {
            // 校验工作流
            SingleResponse<?> validateResponse = workflowValidationHelper.validateWorkflow(workflowId, userId, WorkflowStatusEnum.INIT);
            if (!validateResponse.isSuccess()) {
                return validateResponse;
            }

            // 存储file
            fileDomainService.saveFile(workflowId, files);
            return SingleResponse.buildSuccess();
        } catch (Exception e) {
            // TODO@chai 增加日志
            return SingleResponse.buildFailure("", "");
        }
    }
}
