package com.taichu.gateway.web.aspect;

import com.taichu.common.common.util.RequestContext;
import com.taichu.common.common.util.SnowflakeIdGenerator;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;

@Aspect
@Component
@Order(1)
public class RequestIdAspect {
    private SnowflakeIdGenerator idGenerator;

    @PostConstruct
    public void init() {
        // datacenterId 和 machineId 可根据实际情况配置
        this.idGenerator = new SnowflakeIdGenerator(1, 1);
    }

    @Around("execution(public * com.taichu.gateway.web..*Controller.*(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        String requestId = idGenerator.nextId();
        RequestContext.setRequestId(requestId);
        try {
            Object result = joinPoint.proceed();
            // 设置 response header
            setRequestIdToHeader(requestId);
            return result;
        } finally {
            RequestContext.clear();
        }
    }

    private void setRequestIdToHeader(String requestId) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes) {
            HttpServletResponse response = ((ServletRequestAttributes) requestAttributes).getResponse();
            if (response != null) {
                response.setHeader("X-Request-Id", requestId);
            }
        }
    }
} 