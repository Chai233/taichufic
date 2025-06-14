package com.taichu.domain.enums;

public enum RelevanceType {
    SCRIPT_ID("script_id", "剧本片段ID"),
    ROLE_ID("role_id", "角色ID"),
    STORYBOARD_ID("storyboard_id", "分镜ID"),
    WORKFLOW_ID("workflow_id", "工作流ID");

    private final String value;
    private final String description;

    RelevanceType(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }
}
