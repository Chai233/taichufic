package com.taichu.infra.domain.gateway;

import com.taichu.domain.algo.gateway.AlgoGateway;
import com.taichu.domain.algo.model.*;
import com.taichu.domain.algo.model.request.*;
import com.taichu.domain.algo.model.response.*;
import com.taichu.domain.enums.TaskStatusEnum;
import com.taichu.domain.model.AlgoTaskStatus;
import com.taichu.infra.http.AlgoHttpClient;
import com.taichu.infra.http.AlgoHttpException;
import com.taichu.infra.http.FileResponse;
import com.taichu.common.common.model.ByteArrayMultipartFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 算法服务网关实现类
 * 负责与算法服务进行HTTP通信，处理各种AI任务的创建和结果获取
 */
@Slf4j
@Component
@Profile("!mock")
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
            AlgoApiResponse<TaskIdData> apiResp = algoHttpClient.postMultipart(
                AlgoPathEnum.GENERATE_SCRIPT.getPath(),
                request.getPrompt(),
                request.getWorkflowId(),
                request.getFiles(),
                AlgoApiResponse.class
            );
            return convertApiResponse(apiResp, "ALGO_SCRIPT_CREATE_ERROR");
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
            return algoHttpClient.get(AlgoPathEnum.GET_SCRIPT.getPath(taskId), ScriptResult.class);
        } catch (AlgoHttpException e) {
            ScriptResult result = new ScriptResult();
            result.setTaskId(taskId);
            log.error("getScriptResult error", e);
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
    public StoryboardTextResult createStoryboardTextTask(StoryboardTextRequest request) {
        try {
            AlgoApiResponse<StoryboardTextResult> apiResp = algoHttpClient.post(
                AlgoPathEnum.GENERATE_STORYBOARD.getPath(),
                request,
                AlgoApiResponse.class
            );
            if (apiResp.getCode() != 200) {
                log.error("createStoryboardTextTask error, workflowId: {}, error: {}", request.getWorkflow_id(), apiResp.getMsg());
                return null;
            }
            return apiResp.getData();
        } catch (AlgoHttpException e) {
            log.error("createStoryboardTextTask error, workflowId: {}", request.getWorkflow_id(), e);
            return null;
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
            AlgoApiResponse<TaskIdData> apiResp = algoHttpClient.post(
                AlgoPathEnum.GENERATE_STORYBOARD_IMAGE.getPath(),
                request,
                AlgoApiResponse.class
            );
            return convertApiResponse(apiResp, "ALGO_STORYBOARD_IMAGE_CREATE_ERROR");
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
            String path = AlgoPathEnum.GET_STORYBOARD_IMAGE.getPath(taskId);
            FileResponse imageData = algoHttpClient.downloadFile(path);
            
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
            AlgoApiResponse<TaskIdData> apiResp = algoHttpClient.post(
                AlgoPathEnum.GENERATE_STORYBOARD_VIDEO.getPath(),
                request,
                AlgoApiResponse.class
            );
            return convertApiResponse(apiResp, "ALGO_STORYBOARD_VIDEO_CREATE_ERROR");
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
            String path = AlgoPathEnum.GET_STORYBOARD_VIDEO.getPath(taskId);
            FileResponse videoData = algoHttpClient.downloadFile(path);
            
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
            AlgoApiResponse<TaskIdData> apiResp = algoHttpClient.post(
                AlgoPathEnum.MERGE_VIDEO.getPath(),
                request,
                AlgoApiResponse.class
            );
            return convertApiResponse(apiResp, "ALGO_VIDEO_MERGE_CREATE_ERROR");
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
            String path = AlgoPathEnum.GET_MERGED_VIDEO.getPath(taskId);
            FileResponse videoData = algoHttpClient.downloadFile(path);
            
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
            // 使用新的状态处理方法，支持特殊状态码
            AlgoHttpClient.StatusResponse<AlgoApiResponse> statusResponse = algoHttpClient.getWithStatusHandling(
                AlgoPathEnum.CHECK_TASK_STATUS.getPath(taskId), 
                AlgoApiResponse.class
            );
            
            AlgoTaskStatus status = new AlgoTaskStatus();


            // 根据HTTP状态码映射到任务状态
            int statusCode = statusResponse.getResponse() != null ?
                    statusResponse.getResponse().getCode() : statusResponse.getStatusCode();
            switch (statusCode) {
                case 200:
                    status.setCode(TaskStatusEnum.COMPLETED.getCode());
                    break;
                case 400:
                    status.setCode(TaskStatusEnum.FAILED.getCode());
                    break;
                case 410:
                case 411:
                case 412:
                    status.setCode(TaskStatusEnum.RUNNING.getCode());
                    break;
                default:
                    status.setCode((byte) -1); // 查询失败
                    break;
            }
            
            return status;
            
        } catch (AlgoHttpException e) {
            log.error("checkTaskStatus error, taskId: {}", taskId, e);
            AlgoTaskStatus status = new AlgoTaskStatus();
            status.setCode((byte) -1); // 查询失败
            return status;
        }
    }

    @Override
    public AlgoResponse createRoleImageTask(RoleImageRequest request) {
        try {
            AlgoApiResponse<TaskIdData> apiResp = algoHttpClient.post(
                AlgoPathEnum.GENERATE_ROLE_IMG.getPath(),
                request,
                AlgoApiResponse.class
            );
            return convertApiResponse(apiResp, "ALGO_ROLE_IMAGE_CREATE_ERROR");
        } catch (AlgoHttpException e) {
            AlgoResponse response = new AlgoResponse();
            response.setSuccess(false);
            response.setErrorCode("ALGO_ROLE_IMAGE_CREATE_ERROR_" + e.getStatusCode());
            response.setErrorMsg(e.getMessage());
            return response;
        }
    }

    @Override
    public MultipartFile getRoleImageResult(String taskId) {
        try {
            String path = AlgoPathEnum.GET_ROLE_IMG.getPath(taskId);
            FileResponse imageData = algoHttpClient.downloadFile(path);
            
            if (!imageData.isSuccess()) {
                log.error("获取角色图片失败, taskId: {}, 服务器返回失败", taskId);
                return null;
            }
            
            return new ByteArrayMultipartFile(
                imageData.getData(),
                imageData.getFileName(),
                imageData.getContentType()
            );
        } catch (AlgoHttpException e) {
            log.error("获取角色图片失败, taskId: {}, error: {}", taskId, e.getMessage());
            return null;
        }
    }

    /**
     * 通用算法接口响应转换
     */
    private AlgoResponse convertApiResponse(AlgoApiResponse apiResp, String errorPrefix) {
        AlgoResponse resp = new AlgoResponse();
        if (apiResp == null) {
            resp.setSuccess(false);
            resp.setErrorCode(errorPrefix + "_NULL");
            resp.setErrorMsg("算法服务无响应");
            return resp;
        }
        if (apiResp.getCode() == 200 && apiResp.getData() != null) {
            resp.setSuccess(true);
            Object data = apiResp.getData();
            try {
                // 兼容反序列化为LinkedHashMap的情况
                if (data instanceof TaskIdData) {
                    resp.setTaskId(((TaskIdData) data).getTask_id());
                } else if (data instanceof java.util.Map) {
                    Object tid = ((java.util.Map) data).get("task_id");
                    if (tid != null) resp.setTaskId(tid.toString());
                }
            } catch (Exception ignore) {}
        } else {
            resp.setSuccess(false);
            resp.setErrorCode(errorPrefix + "_" + apiResp.getCode());
            resp.setErrorMsg(apiResp.getMsg());
        }
        return resp;
    }
} 