package com.taichu.application.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EntranceLog {
    /**
     * 业务代码，用于标识当前方法的业务含义
     * 例如：UPLOAD_FILE, CREATE_WORKFLOW 等
     */
    String bizCode() default "";
} 