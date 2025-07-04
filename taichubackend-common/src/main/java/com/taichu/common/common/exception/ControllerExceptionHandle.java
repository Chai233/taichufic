package com.taichu.common.common.exception;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ControllerExceptionHandle {
    /**
     * 业务模块名称
     */
    String biz() default "";
} 