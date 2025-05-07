package com.taichu.application.service;

import com.alibaba.cola.dto.SingleResponse;
import com.taichu.domain.service.FileDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Component
public class FileAppService {

    @Autowired
    private FileDomainService fileDomainService;

    public SingleResponse<?> uploadFiles(List<MultipartFile> files, Long workflowId, Long userId) {
        // 校验当前用户是否为workflow的owner

        // 存储file
        fileDomainService.saveFile(workflowId, files);
        return SingleResponse.buildSuccess();
    }
}
