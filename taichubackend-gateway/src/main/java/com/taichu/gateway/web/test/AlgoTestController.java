package com.taichu.gateway.web.test;

import com.taichu.domain.algo.gateway.AlgoGateway;
import com.taichu.domain.algo.model.AlgoResponse;
import com.taichu.domain.model.AlgoTaskStatus;
import com.taichu.domain.algo.model.common.UploadFile;
import com.taichu.domain.algo.model.request.*;
import com.taichu.domain.algo.model.response.ScriptResult;
import com.taichu.domain.algo.model.response.StoryboardTextResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 算法服务测试控制器
 * 提供对AlgoGateway所有方法的测试接口
 */
@RestController
@RequestMapping("/api/testAlgo")
@Slf4j
public class AlgoTestController {

    @Autowired
    AlgoGateway algoGateway;

    private static final int MAX_RETRY = 3;
    private static final int WAIT_INTERVAL = 5000;

    /**
     * 获取结果的重试方法，专门用于处理获取算法服务结果的操作
     *
     * @param operation 需要重试的获取结果操作
     * @param operationName 操作名称（用于日志）
     * @param taskId 任务ID（用于日志）
     * @param <T> 操作返回类型
     * @return 操作结果，如果重试失败则返回null
     */
    private <T> T retryGetResultOperation(Supplier<T> operation, String operationName, String taskId) {
        int retryCount = 0;
        while (retryCount < MAX_RETRY) {
            try {
                T result = operation.get();
                if (result == null) {
                    log.error("{} returned null for taskId: {}, retry count: {}", operationName, taskId, retryCount + 1);
                    if (retryCount < MAX_RETRY - 1) {
                        // 计算等待时间（指数退避：基础等待时间 * 2^重试次数）
                        long waitTime = calculateWaitTime(retryCount);
                        log.info("{} will retry for taskId: {} after {} ms", operationName, taskId, waitTime);
                        Thread.sleep(waitTime);
                        retryCount++;
                        continue;
                    }
                    return null;
                }
                log.info("{} succeeded for taskId: {} after {} retries", operationName, taskId, retryCount);
                return result;
            } catch (Exception e) {
                log.error("Unexpected error during {} for taskId: {}, retry count: {}", operationName, taskId, retryCount + 1, e);
                if (retryCount < MAX_RETRY - 1) {
                    // 计算等待时间（指数退避：基础等待时间 * 2^重试次数）
                    long waitTime = calculateWaitTime(retryCount);
                    log.info("{} will retry for taskId: {} after {} ms due to exception", operationName, taskId, waitTime);
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Retry interrupted for taskId: {}", taskId);
                        return null;
                    }
                    retryCount++;
                    continue;
                }
                return null;
            }
        }
        log.error("Failed to complete {} for taskId: {} after {} retries", operationName, taskId, MAX_RETRY);
        return null;
    }

    /**
     * 计算重试等待时间（指数退避策略）
     *
     * @param retryCount 当前重试次数
     * @return 等待时间（毫秒）
     */
    private long calculateWaitTime(int retryCount) {
        // 基础等待时间
        long baseWaitTime = WAIT_INTERVAL;
        // 指数退避：基础等待时间 * 2^重试次数，最大不超过30秒
        long waitTime = baseWaitTime * (long) Math.pow(2, retryCount);
        return Math.min(waitTime, 30000); // 最大等待30秒
    }

    /**
     * 判断是否应该重试
     *
     * @param retryCount 当前重试次数
     * @return 是否应该重试
     */
    private boolean shouldRetry(int retryCount) {
        if (retryCount < MAX_RETRY - 1) {
            try {
                Thread.sleep(WAIT_INTERVAL);
                return true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Retry interrupted");
                return false;
            }
        }
        return false;
    }

    /**
     * 测试创建剧本生成任务
     * 
     * @param files 上传的文件列表（支持form-data格式）
     * @param prompt 引导语
     * @param workflowId 工作流ID
     * @return 包含任务ID和状态的响应对象
     */
    @PostMapping("/createScriptTask")
    public ResponseEntity<AlgoResponse> testCreateScriptTask(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("prompt") String prompt,
            @RequestParam("workflowId") String workflowId) {
        
        try {
            // 将MultipartFile转换为UploadFile
            List<UploadFile> uploadFiles = new ArrayList<>();
            for (MultipartFile file : files) {
                UploadFile uploadFile = new UploadFile();
                uploadFile.setFileName(file.getOriginalFilename());
                uploadFile.setFileContent(file.getBytes());
                uploadFile.setContentType(file.getContentType());
                uploadFiles.add(uploadFile);
            }
            
            // 构建ScriptTaskRequest
            ScriptTaskRequest request = new ScriptTaskRequest();
            request.setFiles(uploadFiles);
            request.setPrompt(prompt);
            request.setWorkflowId(workflowId);
            
            AlgoResponse response = algoGateway.createScriptTask(request);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(createErrorResponse("文件处理失败: " + e.getMessage()));
        }
    }

    /**
     * 测试获取剧本生成结果
     * 
     * @param taskId 任务ID
     * @return 包含剧本内容和角色信息的响应对象
     */
    @GetMapping("/getScriptResult/{taskId}")
    public ResponseEntity<ScriptResult> testGetScriptResult(@PathVariable String taskId) {
        ScriptResult result = retryGetResultOperation(
            () -> algoGateway.getScriptResult(taskId),
            "getScriptResult",
            taskId
        );
        return ResponseEntity.ok(result);
    }

    /**
     * 测试创建分镜文本生成任务
     * 
     * @param request 分镜文本生成请求参数，包含剧本片段和工作流ID
     * @return 包含任务ID和状态的响应对象
     */
    @PostMapping("/createStoryboardTextTask")
    public ResponseEntity<StoryboardTextResult> testCreateStoryboardTextTask(@RequestBody StoryboardTextRequest request) {
        StoryboardTextResult res = algoGateway.createStoryboardTextTask(request);
        return ResponseEntity.ok(res);
    }

    /**
     * 测试创建分镜图生成任务
     * 
     * @param request 分镜图生成请求参数，包含分镜描述、角色信息和工作流ID
     * @return 包含任务ID和状态的响应对象
     */
    @PostMapping("/createStoryboardImageTask")
    public ResponseEntity<AlgoResponse> testCreateStoryboardImageTask(@RequestBody StoryboardImageRequest request) {
        AlgoResponse response = algoGateway.createStoryboardImageTask(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 测试获取分镜图生成结果
     * 
     * @param taskId 任务ID
     * @return 分镜图片文件，包含Content-Type和文件名信息
     */
    @GetMapping("/getStoryboardImageResult/{taskId}")
    public ResponseEntity<byte[]> testGetStoryboardImageResult(@PathVariable String taskId) throws IOException {
        MultipartFile file = retryGetResultOperation(
            () -> algoGateway.getStoryboardImageResult(taskId),
            "getStoryboardImageResult",
            taskId
        );
        if (file != null && file.getBytes() != null) {
            return ResponseEntity.ok()
                .header("Content-Type", file.getContentType())
                .header("Content-Disposition", "attachment; filename=\"" + file.getOriginalFilename() + "\"")
                .body(file.getBytes());
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * 测试创建分镜视频生成任务
     * 
     * @param request 分镜视频生成请求参数，包含分镜描述、图片文件和工作流ID
     * @return 包含任务ID和状态的响应对象
     */
    @PostMapping("/createStoryboardVideoTask")
    public ResponseEntity<AlgoResponse> testCreateStoryboardVideoTask(@RequestBody StoryboardVideoRequest request) {
        AlgoResponse response = algoGateway.createStoryboardVideoTask(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 测试获取分镜视频生成结果
     * 
     * @param taskId 任务ID
     * @return 分镜视频文件，包含Content-Type和文件名信息
     */
    @GetMapping("/getStoryboardVideoResult/{taskId}")
    public ResponseEntity<byte[]> testGetStoryboardVideoResult(@PathVariable String taskId) throws IOException {
        MultipartFile file = retryGetResultOperation(
            () -> algoGateway.getStoryboardVideoResult(taskId),
            "getStoryboardVideoResult",
            taskId
        );
        if (file != null && file.getBytes() != null) {
            return ResponseEntity.ok()
                .header("Content-Type", file.getContentType())
                .header("Content-Disposition", "attachment; filename=\"" + file.getOriginalFilename() + "\"")
                .body(file.getBytes());
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * 测试创建视频合成任务
     * 
     * @param request 视频合成请求参数，包含视频文件列表和工作流ID
     * @return 包含任务ID和状态的响应对象
     */
    @PostMapping("/createVideoMergeTask")
    public ResponseEntity<AlgoResponse> testCreateVideoMergeTask(@RequestBody VideoMergeRequest request) {
        AlgoResponse response = algoGateway.createVideoMergeTask(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 测试获取视频合成结果
     * 
     * @param taskId 任务ID
     * @return 合成后的视频文件，包含Content-Type和文件名信息
     */
    @GetMapping("/getVideoMergeResult/{taskId}")
    public ResponseEntity<byte[]> testGetVideoMergeResult(@PathVariable String taskId) throws IOException {
        MultipartFile file = retryGetResultOperation(
            () -> algoGateway.getVideoMergeResult(taskId),
            "getVideoMergeResult",
            taskId
        );
        if (file != null && file.getBytes() != null) {
            return ResponseEntity.ok()
                .header("Content-Type", file.getContentType())
                .header("Content-Disposition", "attachment; filename=\"" + file.getOriginalFilename() + "\"")
                .body(file.getBytes());
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * 测试创建角色图片生成任务
     * 
     * @param request 角色图片生成请求参数，包含角色信息和工作流ID
     * @return 包含任务ID和状态的响应对象
     */
    @PostMapping("/createRoleImageTask")
    public ResponseEntity<AlgoResponse> testCreateRoleImageTask(@RequestBody RoleImageRequest request) {
        AlgoResponse response = algoGateway.createRoleImageTask(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 测试获取角色图片生成结果
     * 
     * @param taskId 任务ID
     * @return 角色图片文件，包含Content-Type和文件名信息
     */
    @GetMapping("/getRoleImageResult/{taskId}")
    public ResponseEntity<byte[]> testGetRoleImageResult(@PathVariable String taskId) throws IOException {
        MultipartFile file = retryGetResultOperation(
            () -> algoGateway.getRoleImageResult(taskId),
            "getRoleImageResult",
            taskId
        );
        if (file != null && file.getBytes() != null) {
            return ResponseEntity.ok()
                .header("Content-Type", file.getContentType())
                .header("Content-Disposition", "attachment; filename=\"" + file.getOriginalFilename() + "\"")
                .body(file.getBytes());
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * 测试查询任务状态
     * 
     * @param taskId 任务ID
     * @return 任务状态信息，包含任务进度和状态码
     */
    @GetMapping("/checkTaskStatus/{taskId}")
    public ResponseEntity<AlgoTaskStatus> testCheckTaskStatus(@PathVariable String taskId) {
        AlgoTaskStatus status = algoGateway.checkTaskStatus(taskId);
        return ResponseEntity.ok(status);
    }

    /**
     * 创建错误响应对象
     * 
     * @param errorMessage 错误信息
     * @return 包含错误信息的响应对象
     */
    private AlgoResponse createErrorResponse(String errorMessage) {
        AlgoResponse response = new AlgoResponse();
        response.setSuccess(false);
        response.setErrorMsg(errorMessage);
        response.setErrorCode("TEST_ERROR");
        return response;
    }
}
