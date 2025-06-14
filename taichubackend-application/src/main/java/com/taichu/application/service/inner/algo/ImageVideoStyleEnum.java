package com.taichu.application.service.inner.algo;

import lombok.Getter;

@Getter
public enum ImageVideoStyleEnum {
    CYBER_PUNK("赛博朋克")
    ;

    private final String value;

    ImageVideoStyleEnum(String value) {
        this.value = value;
    }

}
