package com.taichu.application.aspect;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.taichu.application.annotation.EntranceLog;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Aspect
@Component
@Slf4j
public class LogEntranceAspect {

    private static final int MAX_PARAM_LENGTH = 1000; // 参数最大长度
    private static final String FILE_TYPE_MARK = "[文件类型，不打印具体内容]";
    private static final String ERROR_MARK = "[参数解析错误]";

    @Around("@annotation(entranceLog)")
    public Object around(ProceedingJoinPoint point, EntranceLog entranceLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = point.getSignature().getName();
        String className = point.getTarget().getClass().getName();
        String bizCode = entranceLog.bizCode();
        Object result = null;
        boolean isSuccess = true;
        String errorMsg = null;

        try {
            // 执行方法
            result = point.proceed();
            return result;
        } catch (Throwable e) {
            isSuccess = false;
            errorMsg = e.getMessage();
            throw e;
        } finally {
            // 记录执行时间
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 构建结构化的日志信息
            StringBuilder logBuilder = new StringBuilder();
            logBuilder.append("entrance - bizCode: ").append(bizCode)
                    .append(", method: ").append(className).append(".").append(methodName)
                    .append(", params: ").append(buildParamLog(point))
                    .append(", result: ").append(buildResultLog(result))
                    .append(", executionTime: ").append(executionTime).append("ms")
                    .append(", status: ").append(isSuccess ? "SUCCESS" : "FAILED");
            
            if (!isSuccess && errorMsg != null) {
                logBuilder.append(", error: ").append(errorMsg);
            }
            
            log.info(logBuilder.toString());
        }
    }

    private String buildParamLog(ProceedingJoinPoint point) {
        try {
            Object[] args = point.getArgs();
            if (args == null || args.length == 0) {
                return "{}";
            }

            MethodSignature signature = (MethodSignature) point.getSignature();
            String[] parameterNames = signature.getParameterNames();

            StringBuilder paramLog = new StringBuilder("{");
            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    paramLog.append(", ");
                }

                String paramName = parameterNames[i];
                Object arg = args[i];

                // 处理参数值
                String paramValue = formatParamValue(arg);
                
                // 截断过长的参数值
                if (paramValue.length() > MAX_PARAM_LENGTH) {
                    paramValue = paramValue.substring(0, MAX_PARAM_LENGTH) + "...[已截断]";
                }

                paramLog.append("\"").append(paramName).append("\": ").append(paramValue);
            }
            paramLog.append("}");

            return paramLog.toString();
        } catch (Exception e) {
            log.error("构建参数日志失败", e);
            return ERROR_MARK;
        }
    }

    private String formatParamValue(Object arg) {
        if (arg == null) {
            return "null";
        }

        try {
            // 处理文件类型
            if (arg instanceof MultipartFile) {
                return "\"" + FILE_TYPE_MARK + "\"";
            }
            
            // 处理文件列表
            if (arg instanceof List && !((List<?>) arg).isEmpty() && ((List<?>) arg).get(0) instanceof MultipartFile) {
                return "\"" + FILE_TYPE_MARK + "\"";
            }

            // 处理数组
            if (arg.getClass().isArray()) {
                return Arrays.toString((Object[]) arg);
            }

            // 处理其他类型
            return JSON.toJSONString(arg, SerializerFeature.WriteMapNullValue, 
                    SerializerFeature.WriteNullListAsEmpty, 
                    SerializerFeature.WriteNullStringAsEmpty);
        } catch (Exception e) {
            log.error("格式化参数值失败: {}", arg, e);
            return "\"" + ERROR_MARK + "\"";
        }
    }

    private String buildResultLog(Object result) {
        if (result == null) {
            return "null";
        }

        try {
            String resultStr = JSON.toJSONString(result, SerializerFeature.WriteMapNullValue,
                    SerializerFeature.WriteNullListAsEmpty,
                    SerializerFeature.WriteNullStringAsEmpty);

            // 截断过长的结果
            if (resultStr.length() > MAX_PARAM_LENGTH) {
                return resultStr.substring(0, MAX_PARAM_LENGTH) + "...[已截断]";
            }

            return resultStr;
        } catch (Exception e) {
            log.error("格式化结果失败: {}", result, e);
            return ERROR_MARK;
        }
    }
} 