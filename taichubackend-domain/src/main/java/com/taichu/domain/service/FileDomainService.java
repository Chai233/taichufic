package com.taichu.domain.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface FileDomainService {
    boolean saveFile(Long workflowId, List<MultipartFile> files);
    byte[] read(String relativePath) throws IOException;
}
