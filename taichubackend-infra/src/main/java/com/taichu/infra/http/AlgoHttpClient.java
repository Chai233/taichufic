package com.taichu.infra.http;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * 算法服务 HTTP 客户端工具类
 * 处理与算法服务的 HTTP 通信，包括请求发送和响应接收
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlgoHttpClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${algo.service.base-url}")
    private String baseUrl;
    
    /**
     * 发送 POST 请求
     *
     * @param path 请求路径
     * @param request 请求对象
     * @param responseType 响应类型
     * @return 响应对象
     */
    public <T, R> R post(String path, T request, Class<R> responseType) {
        try {
            String url = baseUrl + path;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<T> entity = new HttpEntity<>(request, headers);
            return restTemplate.postForObject(url, entity, responseType);
        } catch (HttpStatusCodeException e) {
            log.error("HTTP POST 请求失败, path: {}, request: {}, statusCode: {}, responseBody: {}", 
                path, request, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new AlgoHttpException("HTTP POST 请求失败: " + e.getResponseBodyAsString(), e.getStatusCode().value());
        } catch (RestClientException e) {
            log.error("HTTP POST 请求失败, path: {}, request: {}, error: {}", path, request, e.getMessage(), e);
            throw new AlgoHttpException("HTTP POST 请求失败: " + e.getMessage());
        }
    }
    
    /**
     * 发送 GET 请求
     *
     * @param path 请求路径
     * @param responseType 响应类型
     * @return 响应对象
     */
    public <R> R get(String path, Class<R> responseType) {
        try {
            String url = baseUrl + path;
            return restTemplate.getForObject(url, responseType);
        } catch (HttpStatusCodeException e) {
            log.error("HTTP GET 请求失败, path: {}, statusCode: {}, responseBody: {}", 
                path, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new AlgoHttpException("HTTP GET 请求失败: " + e.getResponseBodyAsString(), e.getStatusCode().value());
        } catch (RestClientException e) {
            log.error("HTTP GET 请求失败, path: {}, error: {}", path, e.getMessage(), e);
            throw new AlgoHttpException("HTTP GET 请求失败: " + e.getMessage());
        }
    }
} 