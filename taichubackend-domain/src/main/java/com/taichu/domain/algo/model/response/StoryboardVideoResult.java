package com.taichu.domain.algo.model.response;

import lombok.Data;

@Data
public class StoryboardVideoResult {
    private String taskId;          // 任务ID
    private String errorCode;       // 错误码
    private String errorMsg;        // 错误信息
    private byte[] videoData;       // 视频数据
    private String videoFormat;     // 视频格式（如：mp4, mov等）
    private String videoName;       // 视频名称
} 