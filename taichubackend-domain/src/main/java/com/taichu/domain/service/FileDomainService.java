package com.taichu.domain.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileDomainService {
    boolean saveFile(Long workflowId, MultipartFile file);
}
