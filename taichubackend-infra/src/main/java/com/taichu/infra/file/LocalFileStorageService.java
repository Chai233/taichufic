package com.taichu.infra.file;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Component
public class LocalFileStorageService {

    @Value("${file.storage.base-path:/data/files}")
    private String basePath;

    @Value("${file.storage.max-size:20MB}")
    private String maxSize;

    @Value("#{'${file.storage.allowed-types}'.split(',')}")
    private List<String> allowedTypes;

    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }

        // 验证文件大小
        long maxSizeBytes = parseSize(maxSize);
        if (file.getSize() > maxSizeBytes) {
            throw new RuntimeException("文件大小超过限制：" + maxSize);
        }

        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType)) {
            throw new RuntimeException("不支持的文件类型：" + contentType);
        }

        // 验证文件名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.contains("..")) {
            throw new RuntimeException("文件名不合法");
        }
    }

    private long parseSize(String size) {
        size = size.toUpperCase();
        if (size.endsWith("MB")) {
            return Long.parseLong(size.substring(0, size.length() - 2)) * 1024 * 1024;
        } else if (size.endsWith("KB")) {
            return Long.parseLong(size.substring(0, size.length() - 2)) * 1024;
        } else if (size.endsWith("B")) {
            return Long.parseLong(size.substring(0, size.length() - 1));
        }
        return Long.parseLong(size);
    }

    public String saveFile(MultipartFile file, String relativePath) throws IOException {
        validateFile(file);
        
        // 确保路径安全
        Path fullPath = Paths.get(basePath, relativePath).normalize();
        if (!fullPath.startsWith(Paths.get(basePath).normalize())) {
            throw new RuntimeException("文件路径不合法");
        }

        // 创建父目录
        File parentDir = fullPath.getParent().toFile();
        if (!parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new RuntimeException("创建目录失败：" + parentDir.getAbsolutePath());
            }
        }

        // 保存文件
        Files.write(fullPath, file.getBytes());
        return relativePath;
    }

    public void deleteFile(String relativePath) throws IOException {
        Path fullPath = Paths.get(basePath, relativePath).normalize();
        if (!fullPath.startsWith(Paths.get(basePath).normalize())) {
            throw new RuntimeException("文件路径不合法");
        }
        Files.deleteIfExists(fullPath);
    }

    public byte[] readFile(String relativePath) throws IOException {
        Path fullPath = Paths.get(basePath, relativePath).normalize();
        if (!fullPath.startsWith(Paths.get(basePath).normalize())) {
            throw new RuntimeException("文件路径不合法");
        }
        return Files.readAllBytes(fullPath);
    }
}
