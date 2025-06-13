package com.taichu.infra.domain.gateway;

import com.taichu.domain.algo.gateway.AlgoGateway;
import com.taichu.domain.algo.model.*;
import com.taichu.domain.algo.model.request.*;
import com.taichu.domain.algo.model.response.*;
import com.taichu.domain.model.AlgoTaskStatus;
import com.taichu.infra.http.AlgoHttpClient;
import com.taichu.infra.http.AlgoHttpException;
import com.taichu.infra.http.FileResponse;
import com.taichu.common.common.model.ByteArrayMultipartFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 算法服务网关实现类
 * 负责与算法服务进行HTTP通信，处理各种AI任务的创建和结果获取
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlgoGatewayImpl implements AlgoGateway {
    
    private final AlgoHttpClient algoHttpClient;
    
    /**
     * 创建剧本生成任务
     * 将用户上传的文件和引导语发送给算法服务，生成剧本
     *
     * @param request 包含上传文件、引导语和工作流ID的请求对象
     * @return 包含任务ID和任务状态的响应对象
     */
    @Override
    public AlgoResponse createScriptTask(ScriptTaskRequest request) {
        try {
            return algoHttpClient.post("/api/v1/script/task", request, AlgoResponse.class);
        } catch (AlgoHttpException e) {
            AlgoResponse response = new AlgoResponse();
            response.setSuccess(false);
            response.setErrorCode("ALGO_SCRIPT_CREATE_ERROR_" + e.getStatusCode());
            response.setErrorMsg(e.getMessage());
            return response;
        }
    }

    /**
     * 获取剧本生成任务的结果
     * 根据任务ID查询算法服务，获取生成的剧本内容
     *
     * @param taskId 任务ID
     * @return 包含剧本内容和角色信息的响应对象
     */
    @Override
    public ScriptResult getScriptResult(String taskId) {
        try {
            return algoHttpClient.get("/api/v1/script/result/" + taskId, ScriptResult.class);
        } catch (AlgoHttpException e) {
            ScriptResult result = new ScriptResult();
            result.setTaskId(taskId);
            result.setErrorCode("ALGO_SCRIPT_GET_ERROR_" + e.getStatusCode());
            result.setErrorMsg(e.getMessage());
            return result;
        }
    }
    
    /**
     * 创建分镜文本生成任务
     * 将剧本片段发送给算法服务，生成分镜文本描述
     *
     * @param request 包含剧本片段和工作流ID的请求对象
     * @return 包含任务ID和任务状态的响应对象
     */
    @Override
    public AlgoResponse createStoryboardTextTask(StoryboardTextRequest request) {
        try {
            return algoHttpClient.post("/api/v1/storyboard/text/task", request, AlgoResponse.class);
        } catch (AlgoHttpException e) {
            AlgoResponse response = new AlgoResponse();
            response.setSuccess(false);
            response.setErrorCode("ALGO_STORYBOARD_TEXT_CREATE_ERROR_" + e.getStatusCode());
            response.setErrorMsg(e.getMessage());
            return response;
        }
    }

    /**
     * 获取分镜文本生成任务的结果
     * 根据任务ID查询算法服务，获取生成的分镜文本描述
     *
     * @param taskId 任务ID
     * @return 包含分镜文本和角色信息的响应对象
     */
    @Override
    public StoryboardTextResult getStoryboardTextResult(String taskId) {
        try {
            return algoHttpClient.get("/api/v1/storyboard/text/result/" + taskId, StoryboardTextResult.class);
        } catch (AlgoHttpException e) {
            StoryboardTextResult result = new StoryboardTextResult();
            result.setTaskId(taskId);
            result.setErrorCode("ALGO_STORYBOARD_TEXT_GET_ERROR_" + e.getStatusCode());
            result.setErrorMsg(e.getMessage());
            return result;
        }
    }
    
    /**
     * 创建分镜图生成任务
     * 将分镜文本描述和角色信息发送给算法服务，生成分镜图
     *
     * @param request 包含分镜ID、分镜描述、角色信息和工作流ID的请求对象
     * @return 包含任务ID和任务状态的响应对象
     */
    @Override
    public AlgoResponse createStoryboardImageTask(StoryboardImageRequest request) {
        try {
            return algoHttpClient.post("/api/v1/storyboard/image/task", request, AlgoResponse.class);
        } catch (AlgoHttpException e) {
            AlgoResponse response = new AlgoResponse();
            response.setSuccess(false);
            response.setErrorCode("ALGO_STORYBOARD_IMAGE_CREATE_ERROR_" + e.getStatusCode());
            response.setErrorMsg(e.getMessage());
            return response;
        }
    }

    /**
     * 获取分镜图生成任务的结果
     * 根据任务ID查询算法服务，获取生成的分镜图
     *
     * @param taskId 任务ID
     * @return 包含任务ID和图片数据的响应对象
     */
    @Override
    public MultipartFile getStoryboardImageResult(String taskId) {
        try {
            FileResponse imageData = algoHttpClient.downloadFile("/get_storyboard_image/" + taskId);
            
            if (!imageData.isSuccess()) {
                log.error("获取分镜图片失败, taskId: {}, 服务器返回失败", taskId);
                return null;
            }
            
            // 使用 FileResponse 中的实际文件名和内容类型
            return new ByteArrayMultipartFile(
                imageData.getData(),
                imageData.getFileName(),
                imageData.getContentType()
            );
        } catch (AlgoHttpException e) {
            log.error("获取分镜图片失败, taskId: {}, error: {}", taskId, e.getMessage());
            return null;
        }
    }
    
    /**
     * 创建分镜视频生成任务
     * 将分镜文本描述和图片文件发送给算法服务，生成分镜视频
     *
     * @param request 包含分镜ID、分镜描述、图片文件和工作流ID的请求对象
     * @return 包含任务ID和任务状态的响应对象
     */
    @Override
    public AlgoResponse createStoryboardVideoTask(StoryboardVideoRequest request) {
        try {
            return algoHttpClient.post("/api/v1/storyboard/video/task", request, AlgoResponse.class);
        } catch (AlgoHttpException e) {
            AlgoResponse response = new AlgoResponse();
            response.setSuccess(false);
            response.setErrorCode("ALGO_STORYBOARD_VIDEO_CREATE_ERROR_" + e.getStatusCode());
            response.setErrorMsg(e.getMessage());
            return response;
        }
    }

    /**
     * 获取分镜视频生成任务的结果
     * 根据任务ID查询算法服务，获取生成的分镜视频
     *
     * @param taskId 任务ID
     * @return 包含任务ID和视频数据的响应对象
     */
    @Override
    public MultipartFile getStoryboardVideoResult(String taskId) {
        try {
            FileResponse videoData = algoHttpClient.downloadFile("/get_storyboard_video/" + taskId);
            
            if (!videoData.isSuccess()) {
                log.error("获取分镜视频失败, taskId: {}, 服务器返回失败", taskId);
                return null;
            }
            
            // 使用 FileResponse 中的实际文件名和内容类型
            return new ByteArrayMultipartFile(
                videoData.getData(),
                videoData.getFileName(),
                videoData.getContentType()
            );
        } catch (AlgoHttpException e) {
            log.error("获取分镜视频失败, taskId: {}, error: {}", taskId, e.getMessage());
            return null;
        }
    }
    
    /**
     * 创建视频合成任务
     * 将多个视频文件发送给算法服务，进行视频合成
     *
     * @param request 包含视频文件列表和工作流ID的请求对象
     * @return 包含任务ID和任务状态的响应对象
     */
    @Override
    public AlgoResponse createVideoMergeTask(VideoMergeRequest request) {
        try {
            return algoHttpClient.post("/api/v1/video/merge/task", request, AlgoResponse.class);
        } catch (AlgoHttpException e) {
            AlgoResponse response = new AlgoResponse();
            response.setSuccess(false);
            response.setErrorCode("ALGO_VIDEO_MERGE_CREATE_ERROR_" + e.getStatusCode());
            response.setErrorMsg(e.getMessage());
            return response;
        }
    }

    /**
     * 获取视频合成任务的结果
     * 根据任务ID查询算法服务，获取合成的视频
     *
     * @param taskId 任务ID
     * @return 包含任务ID和视频数据的响应对象
     */
    @Override
    public MultipartFile getVideoMergeResult(String taskId) {
        try {
            FileResponse videoData = algoHttpClient.downloadFile("/get_video_merge/" + taskId);
            
            if (!videoData.isSuccess()) {
                log.error("获取合成视频失败, taskId: {}, 服务器返回失败", taskId);
                return null;
            }
            
            // 使用 FileResponse 中的实际文件名和内容类型
            return new ByteArrayMultipartFile(
                videoData.getData(),
                videoData.getFileName(),
                videoData.getContentType()
            );
        } catch (AlgoHttpException e) {
            log.error("获取合成视频失败, taskId: {}, error: {}", taskId, e.getMessage());
            return null;
        }
    }
    
    /**
     * 检查任务状态
     * 根据任务ID查询算法服务，获取任务的当前状态
     *
     * @param taskId 任务ID
     * @return 任务状态对象
     */
    @Override
    public AlgoTaskStatus checkTaskStatus(String taskId) {
        try {
            return algoHttpClient.get("/api/v1/task/status/" + taskId, AlgoTaskStatus.class);
        } catch (AlgoHttpException e) {
            AlgoTaskStatus status = new AlgoTaskStatus();
            status.setCode((byte) -1);  // 使用-1表示查询失败
            return status;
        }
    }
} 