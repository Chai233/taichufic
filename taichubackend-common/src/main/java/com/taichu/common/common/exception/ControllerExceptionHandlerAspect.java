package com.taichu.common.common.exception;

import com.alibaba.cola.dto.SingleResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
public class ControllerExceptionHandlerAspect {

    @Around("@annotation(com.taichu.common.common.exception.ControllerExceptionHandle)")
    public Object handleControllerException(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        ControllerExceptionHandle annotation = method.getAnnotation(ControllerExceptionHandle.class);
        String biz = annotation.biz();
        try {
            return joinPoint.proceed();
        } catch (BusinessException e) {
            log.warn("[{}] 业务异常: {}", biz, e.getMessage());
            return SingleResponse.buildFailure(e.getCode(), e.getMessage());
        } catch (MissingServletRequestParameterException e) {
            log.warn("[{}] 缺少请求参数: {}", biz, e.getMessage());
            return SingleResponse.buildFailure("400", "缺少必要的请求参数: " + e.getParameterName());
        } catch (MethodArgumentTypeMismatchException e) {
            log.warn("[{}] 参数类型不匹配: {}", biz, e.getMessage());
            return SingleResponse.buildFailure("400", "参数类型不匹配: " + e.getName());
        } catch (HttpRequestMethodNotSupportedException e) {
            log.warn("[{}] 不支持的HTTP方法: {}", biz, e.getMessage());
            return SingleResponse.buildFailure("405", "不支持的HTTP方法: " + e.getMethod());
        } catch (Exception e) {
            log.error("[{}] 处理请求时发生未知异常: {}", biz, e.getMessage(), e);
            return SingleResponse.buildFailure("500", "服务器内部错误");
        }
    }
} 