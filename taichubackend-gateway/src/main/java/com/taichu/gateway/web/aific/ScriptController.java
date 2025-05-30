package com.taichu.gateway.web.aific;

import com.alibaba.cola.dto.MultiResponse;
import com.alibaba.cola.dto.SingleResponse;
import com.taichu.gateway.model.GenerateScriptRequest;
import com.taichu.gateway.model.ScriptDTO;
import com.taichu.gateway.model.StoryboardTaskStatusDTO;
import com.taichu.gateway.model.TaskStatusDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/script")
@Api(tags = "Page 2 - 剧本接口")
public class ScriptController {

    @PostMapping("/generate")
    @ApiOperation(value = "提交剧本生成任务", notes = "")
    public SingleResponse<Long> generateScript(@RequestBody GenerateScriptRequest request) {
        // 提交剧本生成任务
        return SingleResponse.buildSuccess();
    }

    @GetMapping("/task/status")
    @ApiOperation(value = "查询任务状态", notes = "")
    public SingleResponse<TaskStatusDTO> getScriptTaskStatus(@RequestParam("taskId") Long taskId) {
        // 轮询任务结果
        return SingleResponse.buildSuccess();
    }

    @GetMapping("/getScript")
    @ApiOperation(value = "获取剧本", notes = "")
    public MultiResponse<ScriptDTO> getScript(@RequestParam Long workflowId) {
        // 获取剧本
        return MultiResponse.buildSuccess();
    }

    @GetMapping("/downloadScript")
    @ApiOperation(value = "下载剧本", notes = "")
    public ResponseEntity<Resource> downloadScript(@RequestParam Long workflowId) {
        // 下载剧本
        return ResponseEntity.ok().build();
    }
}

