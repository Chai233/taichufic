package com.taichu.domain.algo.model.request;

import com.taichu.domain.algo.model.common.UploadFile;
import lombok.Data;
import java.util.List;

@Data
public class VideoMergeRequest {
    /**
     * 必须：是
     * 工作流id
     */
    private String workflow_id;
    /**
     * 必须：是
     * 需要合并的分镜id列表
     */
    private List<String> storyboard_ids;

    /**
     * 必须：否
     * 旁白配音风格，默认“磁性男声”
     */
    private String voice_type;

    /**
     * 必须：是
     * bgm风格
     */
    private String bgm_type;
} 