package com.taichu.domain.algo.model.request;

import com.taichu.domain.algo.model.common.UploadFile;
import lombok.Data;
import java.util.List;

@Data
public class ScriptTaskRequest {
    private List<UploadFile> files;  // 上传的文件列表
    private String prompt;           // 引导语
    private String workflowId;       // 工作流ID
} 