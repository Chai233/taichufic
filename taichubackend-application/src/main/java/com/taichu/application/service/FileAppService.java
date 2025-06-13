package com.taichu.application.service;

import com.alibaba.cola.dto.SingleResponse;
import com.taichu.application.helper.WorkflowValidationHelper;
import com.taichu.domain.algo.gateway.FileGateway;
import com.taichu.domain.enums.WorkflowStatusEnum;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Component
public class FileAppService {

    @Autowired
    private FileGateway fileGateway;

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
            // TODO@chai fix me!
//            fileDomainService.saveFile(workflowId, files);
            return SingleResponse.buildSuccess();
        } catch (Exception e) {
            // TODO@chai 增加日志
            return SingleResponse.buildFailure("", "");
        }
    }
}
