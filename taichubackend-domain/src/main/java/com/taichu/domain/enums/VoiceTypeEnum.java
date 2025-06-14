package com.taichu.domain.enums;

import lombok.Getter;

@Getter
public enum VoiceTypeEnum {
    DEFAULT_MAN_SOUND("磁性男声")
    ;

    final String value;

    VoiceTypeEnum(String value) {
        this.value = value;
    }

}
