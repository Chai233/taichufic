package com.taichu.domain.algo.gateway;

import org.springframework.web.multipart.MultipartFile;

import com.taichu.common.common.model.Resp;

import java.io.InputStream;

public interface FileGateway {
    /**
     * @param fileName
     * @param files
     * @return  返回对象名称
     */
    Resp<String> saveFile(String fileName, MultipartFile files);

    Resp<String> getFileUrl(String fileObjName);

    byte[] getFileObj(String fileObjName);

    InputStream getFileStream(String fileObjName);
}
