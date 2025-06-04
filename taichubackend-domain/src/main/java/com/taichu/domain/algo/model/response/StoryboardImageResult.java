package com.taichu.domain.algo.model.response;

import lombok.Data;

@Data
public class StoryboardImageResult {
    private String taskId;          // 任务ID
    private String errorCode;       // 错误码
    private String errorMsg;        // 错误信息
    private byte[] imageData;       // 图片数据
    private String imageFormat;     // 图片格式（如：jpg, png等）
    private String imageName;       // 图片名称
} 