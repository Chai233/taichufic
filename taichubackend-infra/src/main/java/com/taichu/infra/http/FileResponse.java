package com.taichu.infra.http;

import lombok.Getter;

// 2. 文件响应对象
@Getter
public class FileResponse {
    // Getters
    private final byte[] data;
    private final String fileName;
    private final String contentType;
    private final boolean success;

    public FileResponse(byte[] data, String fileName, String contentType, boolean success) {
        this.data = data;
        this.fileName = fileName;
        this.contentType = contentType;
        this.success = success;
    }

    // 便利方法：检查是否为图片
    public boolean isImage() {
        return contentType != null && contentType.startsWith("image/");
    }

    // 便利方法：获取文件大小
    public long getSize() {
        return data != null ? data.length : 0;
    }
}
