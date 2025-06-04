package com.taichu.domain.algo.model.request;

import com.taichu.domain.algo.model.common.UploadFile;
import lombok.Data;

@Data
public class StoryboardVideoRequest {
    private String storyboardId;  // 分镜ID
    private String storyboard;    // 分镜描述文本
    private UploadFile image;     // 图片文件
    private String workflowId;    // 工作流ID
} 