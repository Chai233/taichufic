package com.taichu.sdk.model.request;

import lombok.Data;

@Data
public class WorkflowRollBackRequest {

    /**
     * 工作流id
     */
    Long workflowId;

    /**
     * INIT_WAIT_FOR_FILE
     *     ,UPLOAD_FILE_DONE
     *     ,STORYBOARD_IMG_GEN_DONE
     *     ,STORYBOARD_VIDEO_GEN_DONE
     */
    String targetStatus;
}
