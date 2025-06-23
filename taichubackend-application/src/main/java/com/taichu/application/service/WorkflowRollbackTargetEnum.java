package com.taichu.application.service;

public enum WorkflowRollbackTargetEnum {

    INIT_WAIT_FOR_FILE
    ,UPLOAD_FILE_DONE
    ,STORYBOARD_IMG_GEN_DONE
    ,STORYBOARD_VIDEO_GEN_DONE
    ;

    public static WorkflowRollbackTargetEnum  findByValue(String value) throws Exception {
        for (WorkflowRollbackTargetEnum e : WorkflowRollbackTargetEnum.values()) {
            if (e.name().equals(value)) {
                return e;
            }
        }
        throw new Exception("unknown WorkflowRollbackTargetEnum: " + value);
    }
}
