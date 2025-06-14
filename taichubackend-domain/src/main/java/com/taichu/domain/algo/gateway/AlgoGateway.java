package com.taichu.domain.algo.gateway;

import com.taichu.domain.algo.model.*;
import com.taichu.domain.algo.model.request.*;
import com.taichu.domain.algo.model.response.*;
import com.taichu.domain.model.AlgoTaskStatus;
import org.springframework.web.multipart.MultipartFile;

/**
 * 算法服务网关接口
 */
public interface AlgoGateway {
    // 剧本生成相关
    AlgoResponse createScriptTask(ScriptTaskRequest request);
    ScriptResult getScriptResult(String taskId);
    
    // 分镜文本相关
    AlgoResponse createStoryboardTextTask(StoryboardTextRequest request);
    StoryboardTextResult getStoryboardTextResult(String taskId);
    
    // 分镜图相关
    AlgoResponse createStoryboardImageTask(StoryboardImageRequest request);
    MultipartFile getStoryboardImageResult(String taskId);
    
    // 分镜视频相关
    AlgoResponse createStoryboardVideoTask(StoryboardVideoRequest request);
    MultipartFile getStoryboardVideoResult(String taskId);
    
    // 视频合成相关
    AlgoResponse createVideoMergeTask(VideoMergeRequest request);
    MultipartFile getVideoMergeResult(String taskId);
    
    // 任务状态查询
    AlgoTaskStatus checkTaskStatus(String taskId);

    AlgoResponse createRoleImageTask(RoleImageRequest request);
    MultipartFile getRoleImageResult(String taskId);
} 