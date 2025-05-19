package com.taichu.domain.enums;

public enum FicResourceTypeEnum {
    NOVEL("novel"),
    STORYBOARD_IMG("storyboard_img"),
    STORYBOARD_VIDEO("storyboard_video"),
    FULL_VIDEO("full_video");
    ;

    private final String value;

    FicResourceTypeEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
    
}
