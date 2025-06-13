package com.taichu.domain.algo.gateway;

import org.springframework.web.multipart.MultipartFile;

import com.taichu.common.common.model.Resp;

public interface FileGateway {
    Resp<String> saveFile(String fileName, MultipartFile files);

    Resp<String> getFileUrl(String fileObjName);

    byte[] getFeadObj(String fileObjName);
}
