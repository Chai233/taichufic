package com.taichu.domain.algo.gateway;

import com.taichu.domain.algo.model.*;
import com.taichu.domain.algo.model.request.*;
import com.taichu.domain.algo.model.response.*;
import com.taichu.domain.model.TaskStatus;

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
    StoryboardImageResult getStoryboardImageResult(String taskId);
    
    // 分镜视频相关
    AlgoResponse createStoryboardVideoTask(StoryboardVideoRequest request);
    StoryboardVideoResult getStoryboardVideoResult(String taskId);
    
    // 视频合成相关
    AlgoResponse createVideoMergeTask(VideoMergeRequest request);
    VideoMergeResult getVideoMergeResult(String taskId);
    
    // 任务状态查询
    TaskStatus checkTaskStatus(String taskId);
} 