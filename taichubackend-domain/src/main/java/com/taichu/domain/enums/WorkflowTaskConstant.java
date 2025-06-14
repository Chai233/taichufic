package com.taichu.domain.enums;

public interface WorkflowTaskConstant {




    /**
     * 分镜ID
     */
    String STORYBOARD_ID = "storyboardId";


    /* ================================================ */
    /* ================== 生成剧本参数 ================== */
    /* ================================================ */

    String SCRIPT_PROMPT = "prompt";


    /* ================================================ */
    /* ================== 生成图片参数 ================== */
    /* ================================================ */

    /**
     * 画面风格，默认“赛博朋克”
     */
    String IMG_IMAGE_STYLE = "image_style";

    /**
     * 文本引导强度，默认为None，代表使用算法的默认scale值，取值范围是0~1的一个小数
     */
    String IMG_SCALE = "scale";

    /**
     * 风格引导强度，默认为None，代表使用算法的默认scale值，取值范围是0~1的一个小数
     */
    String IMG_STYLE_SCALE = "style_scale";


    /* ================================================ */
    /* ================== 生成视频参数 ================== */
    /* ================================================ */

    String VIDEO_BGM_TYPE = "bgm_type";

    String VIDEO_VOICE_TYPE = "voice_type";
}
