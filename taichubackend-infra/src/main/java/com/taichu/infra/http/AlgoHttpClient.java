package com.taichu.infra.http;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taichu.domain.algo.model.AlgoApiResponse;
import com.taichu.domain.algo.model.common.UploadFile;
import com.taichu.domain.algo.model.response.ScriptResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * 算法服务 HTTP 客户端工具类
 * 处理与算法服务的 HTTP 通信，包括请求发送和响应接收
 */
@Slf4j
@Component
public class AlgoHttpClient {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public AlgoHttpClient(@Qualifier("algoRestTemplate") RestTemplate restTemplate,
                          @Value("${algo.service.base-url}") String baseUrl,
                          ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.objectMapper = objectMapper;
    }
    
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
        String url = baseUrl + path;
        try {
            AlgoApiResponse<R> scriptResult = JSONObject.parseObject(restTemplate.getForObject(url, String.class), new TypeReference<AlgoApiResponse<R>>(responseType) {
            });
            // TODO 空结果和code != 200 处理
            return scriptResult.getData();
        } catch (HttpStatusCodeException e) {
            log.error("HTTP GET 请求失败, path: {}, statusCode: {}, responseBody: {}",
                    url, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new AlgoHttpException("HTTP GET 请求失败: " + e.getResponseBodyAsString(), e.getStatusCode().value());
        } catch (RestClientException e) {
            log.error("HTTP GET 请求失败, path: {}, error: {}", url, e.getMessage(), e);
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

            return new FileResponse(response.getBody(), fileName, contentType, true);

        } catch (HttpStatusCodeException e) {
            log.error("HTTP 文件下载请求失败, path: {}, statusCode: {}, responseBody: {}",
                    path, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new AlgoHttpException("HTTP 文件下载请求失败: " + e.getResponseBodyAsString(), e.getStatusCode().value());
        } catch (RestClientException e) {
            log.error("HTTP 文件下载请求失败, path: {}, error: {}", path, e.getMessage(), e);
            throw new AlgoHttpException("HTTP 文件下载请求失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("HTTP 文件下载请求失败, path: {}, error: {}", path, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 从响应头中提取文件名
     */
    private String extractFileName(HttpHeaders headers) throws Exception {
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
        throw new Exception("无法提取到文件名");
    }

    public <R> R postMultipart(String path, String prompt, String workflowId, java.util.List<UploadFile> files, Class<R> responseType) {
        try {
            String url = baseUrl + path;
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("prompt", prompt);
            body.add("workflow_id", workflowId);

            if (files != null) {
                for (UploadFile file : files) {
                    ByteArrayResource resource = new ByteArrayResource(file.getFileContent()) {
                        @Override
                        public String getFilename() {
                            return file.getFileName();
                        }
                    };
                    body.add("files", resource);
                }
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<>(body, headers);

            return restTemplate.postForObject(url, httpEntity, responseType);
        } catch (HttpStatusCodeException e) {
            log.error("HTTP MULTIPART POST 请求失败, path: {}, statusCode: {}, responseBody: {}", path, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new AlgoHttpException("HTTP MULTIPART POST 请求失败: " + e.getResponseBodyAsString(), e.getStatusCode().value());
        } catch (RestClientException e) {
            log.error("HTTP MULTIPART POST 请求失败, path: {}, error: {}", path, e.getMessage(), e);
            throw new AlgoHttpException("HTTP MULTIPART POST 请求失败: " + e.getMessage());
        }
    }

    /**
     * 发送 GET 请求（用于状态查询，支持特殊状态码处理）
     * 对于状态查询接口，某些非200状态码是正常的业务状态，不应该抛出异常
     *
     * @param path 请求路径
     * @param responseType 响应类型
     * @return 包含状态码和响应体的对象
     */
    public <R> StatusResponse<R> getWithStatusHandling(String path, Class<R> responseType) {
        String url = baseUrl + path;
        try {
            R response = restTemplate.getForObject(url, responseType);
            return new StatusResponse<>(200, response);
        } catch (HttpStatusCodeException e) {
            // 对于状态查询接口，某些状态码是正常的业务状态
            if (e.getStatusCode().value() == 400 || 
                e.getStatusCode().value() == 410 || 
                e.getStatusCode().value() == 411 || 
                e.getStatusCode().value() == 412) {
                // 这些状态码是正常的业务状态，返回响应体内容
                try {
                    R response = objectMapper.readValue(e.getResponseBodyAsString(), responseType);
                    return new StatusResponse<>(e.getStatusCode().value(), response);
                } catch (Exception parseException) {
                    log.error("解析状态查询响应失败, path: {}, statusCode: {}, responseBody: {}", 
                        url, e.getStatusCode(), e.getResponseBodyAsString(), parseException);
                    throw new AlgoHttpException("解析状态查询响应失败: " + e.getResponseBodyAsString(), e.getStatusCode().value());
                }
            } else {
                // 其他状态码仍然抛出异常
                log.error("HTTP GET 请求失败, path: {}, statusCode: {}, responseBody: {}",
                        url, e.getStatusCode(), e.getResponseBodyAsString(), e);
                throw new AlgoHttpException("HTTP GET 请求失败: " + e.getResponseBodyAsString(), e.getStatusCode().value());
            }
        } catch (RestClientException e) {
            log.error("HTTP GET 请求失败, path: {}, error: {}", url, e.getMessage(), e);
            throw new AlgoHttpException("HTTP GET 请求失败: " + e.getMessage());
        }
    }

    /**
     * 状态响应包装类
     */
    public static class StatusResponse<T> {
        private final int statusCode;
        private final T response;

        public StatusResponse(int statusCode, T response) {
            this.statusCode = statusCode;
            this.response = response;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public T getResponse() {
            return response;
        }
    }
} 