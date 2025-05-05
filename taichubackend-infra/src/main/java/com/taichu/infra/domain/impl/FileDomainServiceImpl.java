package com.taichu.infra.domain.impl;

import com.taichu.domain.service.FileDomainService;
import org.springframework.web.multipart.MultipartFile;

public class FileDomainServiceImpl implements FileDomainService {
    @Override
    public boolean saveFile(Long workflowId, MultipartFile file) {
        return false;
    }
}
