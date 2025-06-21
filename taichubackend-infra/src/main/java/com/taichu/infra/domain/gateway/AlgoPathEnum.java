package com.taichu.infra.domain.gateway;

public enum AlgoPathEnum {

    CHECK_TASK_STATUS("/get_task_status/", true),

    GENERATE_SCRIPT("/generate_scripts", false),
    GET_SCRIPT("/get_scripts/", true),

    GENERATE_ROLE_IMG("/generate_role_image", false),
    GET_ROLE_IMG("/get_role_image/", true),

    // 分镜文本生成是同步接口
    GENERATE_STORYBOARD("/generate_storyboard", false),

    GENERATE_STORYBOARD_IMAGE("/generate_storyboard_image", false),
    GET_STORYBOARD_IMAGE("/get_storyboard_image/", true),

    GENERATE_STORYBOARD_VIDEO("/generate_storyboard_video", false),
    GET_STORYBOARD_VIDEO("/get_storyboard_video/", true),

    MERGE_VIDEO("/merge_video", false),
    GET_MERGED_VIDEO("/get_merged_video/", true),
    ;

    
    AlgoPathEnum(String string, boolean needTaskId) {
        this.path = string;
        this.needTaskId = needTaskId;
    }

    private final String path;
    private final boolean needTaskId;

    public String getPath() {
        return path;
    }

    public String getPath(String taskId) {
        return path + taskId;
    }
}
