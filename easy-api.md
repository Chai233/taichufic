# taichubackend-gateway

exported at 2025-06-14 16:10:49

## Step5ComposeController

Step5ComposeController


---
### generateComposeVideo

> BASIC

**Path:** /api/compose/generate

**Method:** POST

> REQUEST

**Headers:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| Content-Type | application/json | YES |  |

**Request Body:**

| name | type | desc |
| ------------ | ------------ | ------------ |
| workflowId | integer | 必填<br>工作流id |
| voiceType | string | 必填<br>旁白配音风格，默认“磁性男声” |
| bgmType | string | 必填<br>bgm风格 |

**Request Demo:**

```json
{
  "workflowId": 0,
  "voiceType": "",
  "bgmType": ""
}
```



> RESPONSE

**Headers:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| content-type | application/json;charset=UTF-8 | NO |  |

**Body:**

| name | type | desc |
| ------------ | ------------ | ------------ |
| success | boolean |  |
| errCode | string |  |
| errMessage | string |  |
| data | integer |  |

**Response Demo:**

```json
{
  "success": false,
  "errCode": "",
  "errMessage": "",
  "data": 0
}
```




---
### getComposeTaskStatus

> BASIC

**Path:** /api/compose/task/status

**Method:** GET

> REQUEST

**Query:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| taskId |  | YES |  |



> RESPONSE

**Headers:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| content-type | application/json;charset=UTF-8 | NO |  |

**Body:**

| name | type | desc |
| ------------ | ------------ | ------------ |
| success | boolean |  |
| errCode | string |  |
| errMessage | string |  |
| data | object |  |
| &ensp;&ensp;&#124;─taskId | integer | 任务ID |
| &ensp;&ensp;&#124;─status | string | 任务状态 |
| &ensp;&ensp;&#124;─progressRatio | integer | 进度百分比 |

**Response Demo:**

```json
{
  "success": false,
  "errCode": "",
  "errMessage": "",
  "data": {
    "taskId": 0,
    "status": "",
    "progressRatio": 0
  }
}
```




---
### downloadComposeVideo

> BASIC

**Path:** /api/compose/download

**Method:** GET

> REQUEST

**Query:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| workflowId |  | YES |  |



> RESPONSE

**Headers:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| content-type | application/json;charset=UTF-8 | NO |  |

**Body:**

| name | type | desc |
| ------------ | ------------ | ------------ |

**Response Demo:**

```json
{}
```




---
### getComposeVideo

> BASIC

**Path:** /api/compose/getAll

**Method:** GET

> REQUEST

**Query:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| workflowId |  | YES |  |



> RESPONSE

**Headers:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| content-type | application/json;charset=UTF-8 | NO |  |

**Body:**

| name | type | desc |
| ------------ | ------------ | ------------ |
| success | boolean |  |
| errCode | string |  |
| errMessage | string |  |
| data | array |  |
| &ensp;&ensp;&#124;─ | object |  |
| &ensp;&ensp;&ensp;&ensp;&#124;─orderIndex | integer | 排序索引 |
| &ensp;&ensp;&ensp;&ensp;&#124;─thumbnailUrl | string | 缩略图URL |
| &ensp;&ensp;&ensp;&ensp;&#124;─workflowId | integer | 工作流ID |
| &ensp;&ensp;&ensp;&ensp;&#124;─storyboardResourceId | integer | 分镜资源ID |

**Response Demo:**

```json
{
  "success": false,
  "errCode": "",
  "errMessage": "",
  "data": [
    {
      "orderIndex": 0,
      "thumbnailUrl": "",
      "workflowId": 0,
      "storyboardResourceId": 0
    }
  ]
}
```





## Step4VideoController

Step4VideoController


---
### generateVideo

> BASIC

**Path:** /api/v1/video/generate

**Method:** POST

> REQUEST

**Headers:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| Content-Type | application/json | YES |  |

**Request Body:**

| name | type | desc |
| ------------ | ------------ | ------------ |
| workflowId | integer | 流程id |

**Request Demo:**

```json
{
  "workflowId": 0
}
```



> RESPONSE

**Headers:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| content-type | application/json;charset=UTF-8 | NO |  |

**Body:**

| name | type | desc |
| ------------ | ------------ | ------------ |
| success | boolean |  |
| errCode | string |  |
| errMessage | string |  |
| data | integer |  |

**Response Demo:**

```json
{
  "success": false,
  "errCode": "",
  "errMessage": "",
  "data": 0
}
```




---
### getVideoTaskStatus

> BASIC

**Path:** /api/v1/video/task/status

**Method:** GET

> REQUEST

**Query:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| taskId |  | YES |  |



> RESPONSE

**Headers:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| content-type | application/json;charset=UTF-8 | NO |  |

**Body:**

| name | type | desc |
| ------------ | ------------ | ------------ |
| success | boolean |  |
| errCode | string |  |
| errMessage | string |  |
| data | object |  |
| &ensp;&ensp;&#124;─taskId | integer | 任务ID |
| &ensp;&ensp;&#124;─status | string | 任务状态 |
| &ensp;&ensp;&#124;─progressRatio | integer | 进度百分比 |
| &ensp;&ensp;&#124;─completedStoryboardIds | array | 已完成的分镜id |
| &ensp;&ensp;&ensp;&ensp;&#124;─ | integer |  |
| &ensp;&ensp;&#124;─completeCnt | integer | 已完成的分镜数 |
| &ensp;&ensp;&#124;─totalCnt | integer | 分镜/视频总数 |

**Response Demo:**

```json
{
  "success": false,
  "errCode": "",
  "errMessage": "",
  "data": {
    "taskId": 0,
    "status": "",
    "progressRatio": 0,
    "completedStoryboardIds": [
      0
    ],
    "completeCnt": 0,
    "totalCnt": 0
  }
}
```




---
### getAllVideo

> BASIC

**Path:** /api/v1/video/getAll

**Method:** GET

> REQUEST

**Query:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| workflowId |  | YES |  |



> RESPONSE

**Headers:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| content-type | application/json;charset=UTF-8 | NO |  |

**Body:**

| name | type | desc |
| ------------ | ------------ | ------------ |
| success | boolean |  |
| errCode | string |  |
| errMessage | string |  |
| data | array |  |
| &ensp;&ensp;&#124;─ | object |  |
| &ensp;&ensp;&ensp;&ensp;&#124;─orderIndex | integer | 展示顺序，由小到大排序 |
| &ensp;&ensp;&ensp;&ensp;&#124;─thumbnailUrl | string | 视频首帧图片url |
| &ensp;&ensp;&ensp;&ensp;&#124;─storyboardId | integer | 分镜id |
| &ensp;&ensp;&ensp;&ensp;&#124;─storyboardResourceId | integer | 分镜视频资源id |

**Response Demo:**

```json
{
  "success": false,
  "errCode": "",
  "errMessage": "",
  "data": [
    {
      "orderIndex": 0,
      "thumbnailUrl": "",
      "storyboardId": 0,
      "storyboardResourceId": 0
    }
  ]
}
```




---
### getSingleVideo

> BASIC

**Path:** /api/v1/video/getSingle

**Method:** GET

> REQUEST

**Query:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| storyboardId |  | YES |  |



> RESPONSE

**Headers:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| content-type | application/json;charset=UTF-8 | NO |  |

**Body:**

| name | type | desc |
| ------------ | ------------ | ------------ |
| success | boolean |  |
| errCode | string |  |
| errMessage | string |  |
| data | object |  |
| &ensp;&ensp;&#124;─orderIndex | integer | 展示顺序，由小到大排序 |
| &ensp;&ensp;&#124;─thumbnailUrl | string | 视频首帧图片url |
| &ensp;&ensp;&#124;─storyboardId | integer | 分镜id |
| &ensp;&ensp;&#124;─storyboardResourceId | integer | 分镜视频资源id |

**Response Demo:**

```json
{
  "success": false,
  "errCode": "",
  "errMessage": "",
  "data": {
    "orderIndex": 0,
    "thumbnailUrl": "",
    "storyboardId": 0,
    "storyboardResourceId": 0
  }
}
```




---
### regenerateSingleVideo

> BASIC

**Path:** /api/v1/video/regenerate

**Method:** POST

> REQUEST

**Headers:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| Content-Type | application/json | YES |  |

**Request Body:**

| name | type | desc |
| ------------ | ------------ | ------------ |
| workflowId | integer | 工作流id |
| storyboardId | integer | 分镜id |
| paramWenbenyindaoqiangdu | integer | 文本引导强度 |
| paramFenggeqiangdu | integer | 风格强度 |
| userPrompt | string | 用户自定义prompt |

**Request Demo:**

```json
{
  "workflowId": 0,
  "storyboardId": 0,
  "paramWenbenyindaoqiangdu": 0,
  "paramFenggeqiangdu": 0,
  "userPrompt": ""
}
```



> RESPONSE

**Headers:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| content-type | application/json;charset=UTF-8 | NO |  |

**Body:**

| name | type | desc |
| ------------ | ------------ | ------------ |
| success | boolean |  |
| errCode | string |  |
| errMessage | string |  |
| data | integer |  |

**Response Demo:**

```json
{
  "success": false,
  "errCode": "",
  "errMessage": "",
  "data": 0
}
```




---
### downloadVideoZip

> BASIC

**Path:** /api/v1/video/download

**Method:** GET

> REQUEST

**Query:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| workflowId |  | YES |  |



> RESPONSE

**Headers:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| content-type | application/json;charset=UTF-8 | NO |  |

**Body:**

| name | type | desc |
| ------------ | ------------ | ------------ |

**Response Demo:**

```json
{}
```





## Step2ScriptController

Step2ScriptController


---
### generateScript

> BASIC

**Path:** /api/v1/script/generate

**Method:** POST

> REQUEST

**Headers:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| Content-Type | application/json | YES |  |

**Request Body:**

| name | type | desc |
| ------------ | ------------ | ------------ |
| workflowId | integer | 流程id |
| userPrompt | string | 用户自定义prompt |
| tag | string | 标签：赛博朋克/外星文明 |

**Request Demo:**

```json
{
  "workflowId": 0,
  "userPrompt": "",
  "tag": ""
}
```



> RESPONSE

**Headers:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| content-type | application/json;charset=UTF-8 | NO |  |

**Body:**

| name | type | desc |
| ------------ | ------------ | ------------ |
| success | boolean |  |
| errCode | string |  |
| errMessage | string |  |
| data | integer |  |

**Response Demo:**

```json
{
  "success": false,
  "errCode": "",
  "errMessage": "",
  "data": 0
}
```




---
### getScriptTaskStatus

> BASIC

**Path:** /api/v1/script/task/status

**Method:** GET

> REQUEST

**Query:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| workflow_id |  | YES |  |



> RESPONSE

**Headers:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| content-type | application/json;charset=UTF-8 | NO |  |

**Body:**

| name | type | desc |
| ------------ | ------------ | ------------ |
| success | boolean |  |
| errCode | string |  |
| errMessage | string |  |
| data | object |  |
| &ensp;&ensp;&#124;─taskId | integer | 任务ID |
| &ensp;&ensp;&#124;─status | string | 任务状态 |
| &ensp;&ensp;&#124;─progressRatio | integer | 进度百分比 |

**Response Demo:**

```json
{
  "success": false,
  "errCode": "",
  "errMessage": "",
  "data": {
    "taskId": 0,
    "status": "",
    "progressRatio": 0
  }
}
```




---
### getScript

> BASIC

**Path:** /api/v1/script/getScript

**Method:** GET

> REQUEST

**Query:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| workflowId |  | YES |  |



> RESPONSE

**Headers:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| content-type | application/json;charset=UTF-8 | NO |  |

**Body:**

| name | type | desc |
| ------------ | ------------ | ------------ |
| success | boolean |  |
| errCode | string |  |
| errMessage | string |  |
| data | array |  |
| &ensp;&ensp;&#124;─ | object |  |
| &ensp;&ensp;&ensp;&ensp;&#124;─order | integer | 剧本分片顺序（从0开始） |
| &ensp;&ensp;&ensp;&ensp;&#124;─scriptContent | string | 剧本内容 |

**Response Demo:**

```json
{
  "success": false,
  "errCode": "",
  "errMessage": "",
  "data": [
    {
      "order": 0,
      "scriptContent": ""
    }
  ]
}
```




---
### downloadScript

> BASIC

**Path:** /api/v1/script/downloadScript

**Method:** GET

> REQUEST

**Query:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| workflowId |  | YES |  |



> RESPONSE

**Headers:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| content-type | application/json;charset=UTF-8 | NO |  |

**Body:**

| name | type | desc |
| ------------ | ------------ | ------------ |

**Response Demo:**

```json
{}
```





## Step3StoryboardImgController

Step3StoryboardImgController


---
### generateStoryboard

> BASIC

**Path:** /api/v1/storyboard/generate

**Method:** POST

> REQUEST

**Headers:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| Content-Type | application/json | YES |  |

**Request Body:**

| name | type | desc |
| ------------ | ------------ | ------------ |
| workflowId | integer | 工作流Id |
| storyboardId | integer | 分镜id |
| scale | number | 文本引导强度 |
| styleScale | number | 风格强度 |
| userPrompt | string | 用户自定义prompt |
| imageStyle | string | 图片风格 |
| selectBox | object | 用户选定图片范围 |
| &ensp;&ensp;&#124;─x | integer | 框选范围左上角横坐标（以图片左上角为原点） |
| &ensp;&ensp;&#124;─y | integer | 框选范围左上角纵坐标（以图片左上角为原点） |
| &ensp;&ensp;&#124;─width | integer | 框选范围宽度 |
| &ensp;&ensp;&#124;─height | integer | 框选范围高度 |

**Request Demo:**

```json
{
  "workflowId": 0,
  "storyboardId": 0,
  "scale": 0.0,
  "styleScale": 0.0,
  "userPrompt": "",
  "imageStyle": "",
  "selectBox": {
    "x": 0,
    "y": 0,
    "width": 0,
    "height": 0
  }
}
```



> RESPONSE

**Headers:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| content-type | application/json;charset=UTF-8 | NO |  |

**Body:**

| name | type | desc |
| ------------ | ------------ | ------------ |
| success | boolean |  |
| errCode | string |  |
| errMessage | string |  |
| data | integer |  |

**Response Demo:**

```json
{
  "success": false,
  "errCode": "",
  "errMessage": "",
  "data": 0
}
```




---
### getStoryboardTaskStatus

> BASIC

**Path:** /api/v1/storyboard/task/status

**Method:** GET

> REQUEST

**Query:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| taskId |  | YES |  |



> RESPONSE

**Headers:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| content-type | application/json;charset=UTF-8 | NO |  |

**Body:**

| name | type | desc |
| ------------ | ------------ | ------------ |
| success | boolean |  |
| errCode | string |  |
| errMessage | string |  |
| data | object |  |
| &ensp;&ensp;&#124;─taskId | integer | 任务ID |
| &ensp;&ensp;&#124;─status | string | 任务状态 |
| &ensp;&ensp;&#124;─progressRatio | integer | 进度百分比 |
| &ensp;&ensp;&#124;─completedStoryboardIds | array | 已完成的分镜id |
| &ensp;&ensp;&ensp;&ensp;&#124;─ | integer |  |
| &ensp;&ensp;&#124;─completeCnt | integer | 已完成的分镜数 |
| &ensp;&ensp;&#124;─totalCnt | integer | 分镜/视频总数 |

**Response Demo:**

```json
{
  "success": false,
  "errCode": "",
  "errMessage": "",
  "data": {
    "taskId": 0,
    "status": "",
    "progressRatio": 0,
    "completedStoryboardIds": [
      0
    ],
    "completeCnt": 0,
    "totalCnt": 0
  }
}
```




---
### getAllStoryboardImg

> BASIC

**Path:** /api/v1/storyboard/getAll

**Method:** GET

> REQUEST

**Query:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| workflowId |  | YES |  |



> RESPONSE

**Headers:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| content-type | application/json;charset=UTF-8 | NO |  |

**Body:**

| name | type | desc |
| ------------ | ------------ | ------------ |
| success | boolean |  |
| errCode | string |  |
| errMessage | string |  |
| data | array |  |
| &ensp;&ensp;&#124;─ | object |  |
| &ensp;&ensp;&ensp;&ensp;&#124;─orderIndex | integer | 展示顺序，由小到大排序 |
| &ensp;&ensp;&ensp;&ensp;&#124;─thumbnailUrl | string | 分镜缩略图url |
| &ensp;&ensp;&ensp;&ensp;&#124;─imgUrl | string | 分镜图url |
| &ensp;&ensp;&ensp;&ensp;&#124;─storyboardId | integer | 分镜id |
| &ensp;&ensp;&ensp;&ensp;&#124;─storyboardResourceId | integer | 分镜资源id |

**Response Demo:**

```json
{
  "success": false,
  "errCode": "",
  "errMessage": "",
  "data": [
    {
      "orderIndex": 0,
      "thumbnailUrl": "",
      "imgUrl": "",
      "storyboardId": 0,
      "storyboardResourceId": 0
    }
  ]
}
```




---
### getSingleStoryboardImg

> BASIC

**Path:** /api/v1/storyboard/getSingle

**Method:** GET

> REQUEST

**Query:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| workflowId |  | YES |  |
| storyboardId |  | YES |  |



> RESPONSE

**Headers:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| content-type | application/json;charset=UTF-8 | NO |  |

**Body:**

| name | type | desc |
| ------------ | ------------ | ------------ |
| success | boolean |  |
| errCode | string |  |
| errMessage | string |  |
| data | object |  |
| &ensp;&ensp;&#124;─orderIndex | integer | 展示顺序，由小到大排序 |
| &ensp;&ensp;&#124;─thumbnailUrl | string | 分镜缩略图url |
| &ensp;&ensp;&#124;─imgUrl | string | 分镜图url |
| &ensp;&ensp;&#124;─storyboardId | integer | 分镜id |
| &ensp;&ensp;&#124;─storyboardResourceId | integer | 分镜资源id |

**Response Demo:**

```json
{
  "success": false,
  "errCode": "",
  "errMessage": "",
  "data": {
    "orderIndex": 0,
    "thumbnailUrl": "",
    "imgUrl": "",
    "storyboardId": 0,
    "storyboardResourceId": 0
  }
}
```




---
### regenerateSingleStoryboard

> BASIC

**Path:** /api/v1/storyboard/regenerate

**Method:** POST

> REQUEST

**Headers:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| Content-Type | application/json | YES |  |

**Request Body:**

| name | type | desc |
| ------------ | ------------ | ------------ |
| workflowId | integer | 工作流Id |
| storyboardId | integer | 分镜id |
| scale | number | 文本引导强度 |
| styleScale | number | 风格强度 |
| userPrompt | string | 用户自定义prompt |
| imageStyle | string | 图片风格 |
| selectBox | object | 用户选定图片范围 |
| &ensp;&ensp;&#124;─x | integer | 框选范围左上角横坐标（以图片左上角为原点） |
| &ensp;&ensp;&#124;─y | integer | 框选范围左上角纵坐标（以图片左上角为原点） |
| &ensp;&ensp;&#124;─width | integer | 框选范围宽度 |
| &ensp;&ensp;&#124;─height | integer | 框选范围高度 |

**Request Demo:**

```json
{
  "workflowId": 0,
  "storyboardId": 0,
  "scale": 0.0,
  "styleScale": 0.0,
  "userPrompt": "",
  "imageStyle": "",
  "selectBox": {
    "x": 0,
    "y": 0,
    "width": 0,
    "height": 0
  }
}
```



> RESPONSE

**Headers:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| content-type | application/json;charset=UTF-8 | NO |  |

**Body:**

| name | type | desc |
| ------------ | ------------ | ------------ |
| success | boolean |  |
| errCode | string |  |
| errMessage | string |  |
| data | integer |  |

**Response Demo:**

```json
{
  "success": false,
  "errCode": "",
  "errMessage": "",
  "data": 0
}
```




---
### downloadStoryboardZip

> BASIC

**Path:** /api/v1/storyboard/download

**Method:** GET

> REQUEST

**Query:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| workflowId |  | YES |  |



> RESPONSE

**Headers:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| content-type | application/json;charset=UTF-8 | NO |  |

**Body:**

| name | type | desc |
| ------------ | ------------ | ------------ |

**Response Demo:**

```json
{}
```





## WorkflowController

WorkflowController


---
### createWorkflow

> BASIC

**Path:** /api/workflow/create

**Method:** POST

> REQUEST



> RESPONSE

**Headers:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| content-type | application/json;charset=UTF-8 | NO |  |

**Body:**

| name | type | desc |
| ------------ | ------------ | ------------ |
| success | boolean |  |
| errCode | string |  |
| errMessage | string |  |
| data | integer |  |

**Response Demo:**

```json
{
  "success": false,
  "errCode": "",
  "errMessage": "",
  "data": 0
}
```





## Step1FileController

Step1FileController


---
### uploadFiles

> BASIC

**Path:** /api/v1/files/upload

**Method:** POST

> REQUEST

**Headers:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| Content-Type | multipart/form-data | YES |  |

**Query:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| workflowId |  | YES |  |

**Form:**

| name | value | required | type | desc |
| ------------ | ------------ | ------------ | ------------ | ------------ |
| files |  | YES | file |  |



> RESPONSE

**Headers:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| content-type | application/json;charset=UTF-8 | NO |  |

**Body:**

| name | type | desc |
| ------------ | ------------ | ------------ |
| success | boolean |  |
| errCode | string |  |
| errMessage | string |  |
| data | object |  |

**Response Demo:**

```json
{
  "success": false,
  "errCode": "",
  "errMessage": "",
  "data": null
}
```





