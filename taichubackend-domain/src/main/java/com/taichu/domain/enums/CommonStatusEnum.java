package com.taichu.domain.enums;

public enum CommonStatusEnum {
    VALID((byte) 1),
    INVALID((byte) 0);

    private final byte value;

    CommonStatusEnum(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }    
    
}