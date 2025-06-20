package com.taichu.gateway.web.test;

import com.taichu.domain.algo.gateway.AlgoGateway;
import com.taichu.domain.algo.model.AlgoResponse;
import com.taichu.domain.algo.model.request.*;
import com.taichu.domain.algo.model.response.*;
import com.taichu.domain.model.AlgoTaskStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 算法服务测试控制器
 * 提供对AlgoGateway所有方法的测试接口
 */
@RestController
@RequestMapping("/api/testAlgo")
public class AlgoTestController {

    @Autowired
    AlgoGateway algoGateway;

    /**
     * 测试创建剧本生成任务
     * 
     * @param request 剧本生成请求参数，包含上传文件、引导语和工作流ID
     * @return 包含任务ID和状态的响应对象
     */
    @PostMapping("/createScriptTask")
    public ResponseEntity<AlgoResponse> testCreateScriptTask(@RequestBody ScriptTaskRequest request) {
        AlgoResponse response = algoGateway.createScriptTask(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 测试获取剧本生成结果
     * 
     * @param taskId 任务ID
     * @return 包含剧本内容和角色信息的响应对象
     */
    @GetMapping("/getScriptResult/{taskId}")
    public ResponseEntity<ScriptResult> testGetScriptResult(@PathVariable String taskId) {
        ScriptResult result = algoGateway.getScriptResult(taskId);
        return ResponseEntity.ok(result);
    }

    /**
     * 测试创建分镜文本生成任务
     * 
     * @param request 分镜文本生成请求参数，包含剧本片段和工作流ID
     * @return 包含任务ID和状态的响应对象
     */
    @PostMapping("/createStoryboardTextTask")
    public ResponseEntity<AlgoResponse> testCreateStoryboardTextTask(@RequestBody StoryboardTextRequest request) {
        AlgoResponse response = algoGateway.createStoryboardTextTask(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 测试获取分镜文本生成结果
     * 
     * @param taskId 任务ID
     * @return 包含分镜文本和角色信息的响应对象
     */
    @GetMapping("/getStoryboardTextResult/{taskId}")
    public ResponseEntity<StoryboardTextResult> testGetStoryboardTextResult(@PathVariable String taskId) {
        StoryboardTextResult result = algoGateway.getStoryboardTextResult(taskId);
        return ResponseEntity.ok(result);
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
        MultipartFile file = algoGateway.getStoryboardImageResult(taskId);
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
        MultipartFile file = algoGateway.getStoryboardVideoResult(taskId);
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
        MultipartFile file = algoGateway.getVideoMergeResult(taskId);
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
        MultipartFile file = algoGateway.getRoleImageResult(taskId);
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
