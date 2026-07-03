package com.teammatch.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件上传结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResultVO {

    /** OSS 对象 key，如 avatars/123/xxx.jpg */
    private String objectKey;

    /** 持久化 URL（写入数据库） */
    private String storedUrl;

    /** 可访问 URL（私有 Bucket 为带签名的临时链接） */
    private String accessUrl;
}
