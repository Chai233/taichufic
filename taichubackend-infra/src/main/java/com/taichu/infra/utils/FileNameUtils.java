package com.taichu.infra.utils;

import java.util.UUID;

public class FileNameUtils {
    private static final int MAX_FILE_NAME_LENGTH = 100;
    private static final String SPECIAL_CHARS_REGEX = "[\\\\/:*?\"<>|]";
    private static final String REPLACEMENT = "_";

    /**
     * 处理文件名
     * @param originalFileName 原始文件名
     * @return 处理后的文件名
     */
    public static String processFileName(String originalFileName) {
        // 获取文件扩展名
        String extension = "";
        int lastDotIndex = originalFileName.lastIndexOf(".");
        if (lastDotIndex > 0) {
            extension = originalFileName.substring(lastDotIndex);
            originalFileName = originalFileName.substring(0, lastDotIndex);
        }

        // 替换特殊字符
        String processedName = originalFileName.replaceAll(SPECIAL_CHARS_REGEX, REPLACEMENT);
        
        // 截断文件名
        if (processedName.length() > MAX_FILE_NAME_LENGTH) {
            processedName = processedName.substring(0, MAX_FILE_NAME_LENGTH);
        }

        // 生成唯一标识
        String uniqueId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        
        // 拼接文件名
        return processedName + "_" + uniqueId + extension;
    }
} 