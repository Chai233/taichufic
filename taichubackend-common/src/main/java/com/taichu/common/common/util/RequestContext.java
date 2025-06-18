package com.taichu.common.common.util;

import org.slf4j.MDC;

public class RequestContext {
    private static final ThreadLocal<String> REQUEST_ID_HOLDER = new ThreadLocal<>();
    private static final String REQUEST_ID_KEY = "requestId";

    public static void setRequestId(String requestId) {
        REQUEST_ID_HOLDER.set(requestId);
        MDC.put(REQUEST_ID_KEY, requestId);
    }

    public static String getRequestId() {
        return REQUEST_ID_HOLDER.get();
    }

    public static void clear() {
        REQUEST_ID_HOLDER.remove();
        MDC.remove(REQUEST_ID_KEY);
    }
} 