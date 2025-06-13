package com.taichu.common.common.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Resp<T> {
    private T data;
    private boolean success;
    private String message;
    private String code;

    private Resp(T data, boolean success, String code, String message) {
        this.data = data;
        this.success = success;
        this.code = code;
        this.message = message;
    }

    public static <T> Resp<T> success(T data) {
        return new Resp<>(data, true, null, null);
    }

    public static <T> Resp<T> error(String code, String message) {
        return new Resp<>(null, false, code, message);
    }
    
}
