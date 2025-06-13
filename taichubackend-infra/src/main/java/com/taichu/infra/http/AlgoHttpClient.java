package com.taichu.infra.http;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
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

    /**
     * 下载文件（用于处理二进制响应）
     *
     * @param path 请求路径
     * @return 文件响应对象
     */
    public FileResponse downloadFile(String path) {
        try {
            String url = baseUrl + path;

            // 使用 exchange 方法获取完整的响应信息
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    byte[].class
            );

            // 检查状态码
            if (response.getStatusCode() != HttpStatus.OK) {
                log.error("HTTP 文件下载请求失败, path: {}, statusCode: {}, responseBody: {}",
                        path, response.getStatusCode(), new String(response.getBody()));
                return new FileResponse(null, null, null, false);
            }

            // 提取文件名（从 Content-Disposition 头中）
            String fileName = extractFileName(response.getHeaders());

            // 获取内容类型
            String contentType = response.getHeaders().getContentType() != null
                    ? response.getHeaders().getContentType().toString()
                    : "application/octet-stream";

            return new FileResponse(
                    response.getBody(),
                    fileName,
                    contentType,
                    true
            );

        } catch (HttpStatusCodeException e) {
            log.error("HTTP 文件下载请求失败, path: {}, statusCode: {}, responseBody: {}",
                    path, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new AlgoHttpException("HTTP 文件下载请求失败: " + e.getResponseBodyAsString(), e.getStatusCode().value());
        } catch (RestClientException e) {
            log.error("HTTP 文件下载请求失败, path: {}, error: {}", path, e.getMessage(), e);
            throw new AlgoHttpException("HTTP 文件下载请求失败: " + e.getMessage());
        }
    }

    /**
     * 从响应头中提取文件名
     */
    private String extractFileName(HttpHeaders headers) {
        String contentDisposition = headers.getFirst("Content-Disposition");
        if (contentDisposition != null && contentDisposition.contains("filename=")) {
            // 提取 filename="..." 中的文件名
            String[] parts = contentDisposition.split("filename=");
            if (parts.length > 1) {
                String fileName = parts[1].trim();
                // 去掉引号
                if (fileName.startsWith("\"") && fileName.endsWith("\"")) {
                    fileName = fileName.substring(1, fileName.length() - 1);
                }
                return fileName;
            }
        }
        // 如果无法提取文件名，生成一个默认名称
        return "storyboard_" + System.currentTimeMillis() + ".png";
    }
} 