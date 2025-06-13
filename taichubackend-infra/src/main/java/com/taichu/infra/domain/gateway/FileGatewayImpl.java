package com.taichu.infra.domain.gateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.taichu.common.common.model.Resp;
import com.taichu.domain.algo.gateway.FileGateway;
import com.taichu.infra.file.OssStorageService;
import com.taichu.infra.utils.FileNameUtils;

import java.io.IOException;
import java.io.InputStream;

@Component
public class FileGatewayImpl implements FileGateway {

    @Autowired
    private OssStorageService ossStorageService;

    @Override
    public Resp<String> saveFile(String fileName, MultipartFile files) {
        try {
            // 处理文件名
            String processedFileName = FileNameUtils.processFileName(fileName);
            
            // 上传文件
            String fileUrl = ossStorageService.uploadFile(files, processedFileName);
            return Resp.success(fileUrl);
        } catch (Exception e) {
            return Resp.error("FILE_UPLOAD_ERROR", "文件上传失败：" + e.getMessage());
        }
    }

    @Override
    public Resp<String> getFileUrl(String fileObjName) {
        try {
            String downloadUrl = ossStorageService.getDownloadUrl(fileObjName);
            return Resp.success(downloadUrl);
        } catch (Exception e) {
            return Resp.error("FILE_URL_ERROR", "获取文件URL失败：" + e.getMessage());
        }
    }

    @Override
    public byte[] getFileObj(String fileObjName) {
        try (InputStream inputStream = ossStorageService.getFileStream(fileObjName)) {
            return inputStream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("获取文件内容失败", e);
        }
    }

    @Override
    public InputStream getFileStream(String fileObjName) {
        return ossStorageService.getFileStream(fileObjName);
    }
}
