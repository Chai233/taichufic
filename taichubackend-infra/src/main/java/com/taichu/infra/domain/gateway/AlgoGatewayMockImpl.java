package com.taichu.infra.domain.gateway;

import com.taichu.common.common.model.ByteArrayMultipartFile;
import com.taichu.domain.algo.gateway.AlgoGateway;
import com.taichu.domain.algo.model.AlgoResponse;
import com.taichu.domain.algo.model.common.RoleDTO;
import com.taichu.domain.algo.model.request.*;
import com.taichu.domain.algo.model.response.ScriptResult;
import com.taichu.domain.algo.model.response.StoryboardTextResult;
import com.taichu.domain.enums.TaskStatusEnum;
import com.taichu.domain.model.AlgoTaskStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Component
@Profile("mock")
@RequiredArgsConstructor
public class AlgoGatewayMockImpl implements AlgoGateway {

    private final Map<String, Long> taskCache = new ConcurrentHashMap<>();
    private static final long EXPIRATION_TIME_MS = 60 * 1000; // 1 minute

    private MultipartFile loadMockFile() {
        try {
            ClassPathResource resource = new ClassPathResource("test/img.png");
            InputStream inputStream = resource.getInputStream();
            byte[] fileContent = StreamUtils.copyToByteArray(inputStream);
            return new ByteArrayMultipartFile(fileContent, "img.png", "image/png");
        } catch (IOException e) {
            log.error("Failed to load mock file", e);
            return null;
        }
    }

    private MultipartFile createMockRoleImagesZip() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(baos);
            
            // 加载mock图片文件
            ClassPathResource resource = new ClassPathResource("test/img.png");
            InputStream inputStream = resource.getInputStream();
            byte[] imageContent = StreamUtils.copyToByteArray(inputStream);
            
            // 创建多个角色图片文件到zip中
            String[] roleNames = {"角色1_正面.png", "角色1_侧面.png", "角色1_背面.png", 
                                 "角色2_正面.png", "角色2_侧面.png", "角色2_背面.png"};
            
            for (String fileName : roleNames) {
                ZipEntry entry = new ZipEntry(fileName);
                zos.putNextEntry(entry);
                zos.write(imageContent);
                zos.closeEntry();
            }
            
            zos.close();
            byte[] zipContent = baos.toByteArray();
            
            return new ByteArrayMultipartFile(zipContent, "role_images.zip", "application/zip");
            
        } catch (IOException e) {
            log.error("Failed to create mock role images zip", e);
            return null;
        }
    }

    private AlgoResponse createTask(String prefix) {
        String taskId = prefix + "-" + UUID.randomUUID();
        taskCache.put(taskId, System.currentTimeMillis());
        log.info("Mock task created: {}. Current cache size: {}", taskId, taskCache.size());
        AlgoResponse response = new AlgoResponse();
        response.setSuccess(true);
        response.setTaskId(taskId);
        return response;
    }

    @Override
    public AlgoResponse createScriptTask(ScriptTaskRequest request) {
        log.info("Mocking createScriptTask with request: {}", request);
        return createTask("mock-script");
    }

    @Override
    public ScriptResult getScriptResult(String taskId) {
        log.info("Mocking getScriptResult for taskId: {}", taskId);
        ScriptResult result = new ScriptResult();
        result.setTaskId(taskId);
        result.setScripts(Collections.singletonList("这是一个mock的剧本内容。"));
        RoleDTO role = new RoleDTO();
        role.setName("角色1");
        role.setPrompt("一个英俊的男人");
        role.setDescription("一个英俊的男人，有着迷人的微笑。");
        result.setRoles(Collections.singletonList(role));
        return result;
    }

    @Override
    public AlgoResponse createStoryboardTextTask(StoryboardTextRequest request) {
        log.info("Mocking createStoryboardTextTask with request: {}", request);
        return createTask("mock-storyboard-text");
    }

    @Override
    public StoryboardTextResult getStoryboardTextResult(String taskId) {
        log.info("Mocking getStoryboardTextResult for taskId: {}", taskId);
        StoryboardTextResult result = new StoryboardTextResult();
        result.setData(Collections.singletonList("这是一个mock的分镜文本。"));
        return result;
    }

    @Override
    public AlgoResponse createStoryboardImageTask(StoryboardImageRequest request) {
        log.info("Mocking createStoryboardImageTask with request: {}", request);
        return createTask("mock-storyboard-image");
    }

    @Override
    public MultipartFile getStoryboardImageResult(String taskId) {
        log.info("Mocking getStoryboardImageResult for taskId: {}", taskId);
        return loadMockFile();
    }

    @Override
    public AlgoResponse createStoryboardVideoTask(StoryboardVideoRequest request) {
        log.info("Mocking createStoryboardVideoTask with request: {}", request);
        return createTask("mock-storyboard-video");
    }

    @Override
    public MultipartFile getStoryboardVideoResult(String taskId) {
        log.info("Mocking getStoryboardVideoResult for taskId: {}", taskId);
        return loadMockFile();
    }

    @Override
    public AlgoResponse createVideoMergeTask(VideoMergeRequest request) {
        log.info("Mocking createVideoMergeTask with request: {}", request);
        return createTask("mock-video-merge");
    }

    @Override
    public MultipartFile getVideoMergeResult(String taskId) {
        log.info("Mocking getVideoMergeResult for taskId: {}", taskId);
        return loadMockFile();
    }

    @Override
    public AlgoTaskStatus checkTaskStatus(String taskId) {
        log.info("Mocking checkTaskStatus for taskId: {}", taskId);
        AlgoTaskStatus status = new AlgoTaskStatus();
        Long creationTime = taskCache.get(taskId);

        if (creationTime == null) {
            // Task not found or already expired, assume completed
            log.info("Task {} not in cache, returning COMPLETED.", taskId);
            status.setCode(TaskStatusEnum.COMPLETED.getCode());
            return status;
        }

        long elapsedTime = System.currentTimeMillis() - creationTime;

        if (elapsedTime > EXPIRATION_TIME_MS) {
            // Expired, so it's completed
            log.info("Task {} expired, returning COMPLETED and removing from cache.", taskId);
            taskCache.remove(taskId); // Clean up
            status.setCode(TaskStatusEnum.COMPLETED.getCode());
        } else {
            // Still running
            log.info("Task {} is still running ({}ms elapsed).", taskId, elapsedTime);
            status.setCode(TaskStatusEnum.RUNNING.getCode());
        }
        return status;
    }

    @Override
    public String ping() {
        log.info("Mocking ping");
        return "pong from mock";
    }

    @Override
    public AlgoResponse createRoleImageTask(RoleImageRequest request) {
        log.info("Mocking createRoleImageTask with request: {}", request);
        return createTask("mock-role-image");
    }

    @Override
    public MultipartFile getRoleImageResult(String taskId) {
        log.info("Mocking getRoleImageResult for taskId: {}", taskId);
        return createMockRoleImagesZip();
    }
} 