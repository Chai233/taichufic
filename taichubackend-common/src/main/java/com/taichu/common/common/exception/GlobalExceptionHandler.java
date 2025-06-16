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
public class GlobalExceptionHandler {

    @Around("@annotation(com.taichu.common.common.exception.GlobalExceptionHandle)")
    public Object handleException(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取注解信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        GlobalExceptionHandle annotation = method.getAnnotation(GlobalExceptionHandle.class);
        String biz = annotation.biz();
        
        try {
            return joinPoint.proceed();
        } catch (BusinessException e) {
            // 处理业务异常
            log.warn("[{}] 业务异常: {}", biz, e.getMessage());
            return SingleResponse.buildFailure(e.getCode(), e.getMessage());
        } catch (MissingServletRequestParameterException e) {
            // 处理缺少请求参数异常
            log.warn("[{}] 缺少请求参数: {}", biz, e.getMessage());
            return SingleResponse.buildFailure("400", "缺少必要的请求参数: " + e.getParameterName());
        } catch (MethodArgumentTypeMismatchException e) {
            // 处理参数类型不匹配异常
            log.warn("[{}] 参数类型不匹配: {}", biz, e.getMessage());
            return SingleResponse.buildFailure("400", "参数类型不匹配: " + e.getName());
        } catch (HttpRequestMethodNotSupportedException e) {
            // 处理不支持的HTTP方法异常
            log.warn("[{}] 不支持的HTTP方法: {}", biz, e.getMessage());
            return SingleResponse.buildFailure("405", "不支持的HTTP方法: " + e.getMethod());
        } catch (Exception e) {
            // 处理其他未知异常
            log.error("[{}] 处理请求时发生未知异常: {}", biz, e.getMessage(), e);
            return SingleResponse.buildFailure("500", "服务器内部错误");
        }
    }
} 