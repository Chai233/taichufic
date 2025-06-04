package com.taichu.domain.algo.model.common;

import lombok.Data;

@Data
public class UploadFile {
    private String fileName;    // 文件名
    private byte[] fileContent; // 文件内容
    private String contentType; // 文件类型
} 