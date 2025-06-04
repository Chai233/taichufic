package com.taichu.domain.algo.model.request;

import com.taichu.domain.algo.model.common.UploadFile;
import lombok.Data;
import java.util.List;

@Data
public class VideoMergeRequest {
    private List<UploadFile> files;  // 视频文件列表
    private String workflowId;       // 工作流ID
} 