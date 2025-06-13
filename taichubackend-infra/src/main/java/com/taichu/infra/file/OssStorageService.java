package com.taichu.infra.file;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.PutObjectRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;

@Slf4j
@Service
public class OssStorageService {

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.accessKeyId}")
    private String accessKeyId;

    @Value("${aliyun.oss.accessKeySecret}")
    private String accessKeySecret;

    @Value("${aliyun.oss.bucketName}")
    private String bucketName;

    private OSS ossClient;

    @PostConstruct
    public void init() {
        ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        log.info("OSS客户端初始化完成");
    }

    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
            log.info("OSS客户端已关闭");
        }
    }

    /**
     * 上传文件到OSS
     *
     * @param file 文件
     * @param objectName OSS中的对象名称
     * @return 文件的访问URL
     */
    public String uploadFile(MultipartFile file, String objectName) {
        try {
            // 创建PutObjectRequest对象
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, file.getInputStream());
            
            // 上传文件
            ossClient.putObject(putObjectRequest);
            
            // 生成文件访问URL
            Date expiration = new Date(System.currentTimeMillis() + 3600L * 1000 * 24 * 365 * 10); // 10年有效期
            URL url = ossClient.generatePresignedUrl(bucketName, objectName, expiration);
            
            return url.toString();
        } catch (Exception e) {
            log.error("上传文件到OSS失败", e);
            throw new RuntimeException("上传文件失败", e);
        }
    }

    /**
     * 从OSS下载文件
     *
     * @param objectName OSS中的对象名称
     * @param localFilePath 本地保存路径
     */
    public void downloadFile(String objectName, String localFilePath) {
        try {
            // 下载文件
            ossClient.getObject(new GetObjectRequest(bucketName, objectName), new File(localFilePath));
        } catch (Exception e) {
            log.error("从OSS下载文件失败", e);
            throw new RuntimeException("下载文件失败", e);
        }
    }

    /**
     * 获取文件输入流
     *
     * @param objectName OSS中的对象名称
     * @return 文件输入流
     */
    public InputStream getFileStream(String objectName) {
        try {
            OSSObject ossObject = ossClient.getObject(bucketName, objectName);
            return ossObject.getObjectContent();
        } catch (Exception e) {
            log.error("获取文件流失败", e);
            throw new RuntimeException("获取文件流失败", e);
        }
    }

    /**
     * 删除OSS中的文件
     *
     * @param objectName OSS中的对象名称
     */
    public void deleteFile(String objectName) {
        try {
            ossClient.deleteObject(bucketName, objectName);
        } catch (Exception e) {
            log.error("删除OSS文件失败", e);
            throw new RuntimeException("删除文件失败", e);
        }
    }

    /**
     * 获取对象的下载链接
     *
     * @param objectName OSS中的对象名称
     * @param expirationMinutes 链接有效期（分钟），默认30分钟
     * @return 带签名的临时访问URL
     */
    public String getDownloadUrl(String objectName, Integer expirationMinutes) {
        try {
            // 设置链接过期时间
            Date expiration = new Date(System.currentTimeMillis() + (expirationMinutes != null ? expirationMinutes : 30) * 60 * 1000L);
            
            // 生成带签名的URL
            URL url = ossClient.generatePresignedUrl(bucketName, objectName, expiration);
            return url.toString();
        } catch (Exception e) {
            log.error("获取下载链接失败", e);
            throw new RuntimeException("获取下载链接失败", e);
        }
    }

    /**
     * 获取对象的下载链接（默认30分钟有效期）
     *
     * @param objectName OSS中的对象名称
     * @return 带签名的临时访问URL
     */
    public String getDownloadUrl(String objectName) {
        return getDownloadUrl(objectName, 30);
    }
}