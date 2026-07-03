package com.teammatch.service.storage;

import com.teammatch.dto.FileUploadResultVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 阿里云 OSS 文件服务
 */
public interface OssService {

    FileUploadResultVO upload(FileCategory category, Long userId, MultipartFile file);

    String resolveAccessibleUrl(String storedUrl);

    List<String> resolveAccessibleUrls(List<String> storedUrls);

    List<String> normalizeStoredUrls(List<String> urls);

    void validateEvidenceUrls(List<String> evidenceUrls, Long userId, FileCategory category);

    /**
     * 头像签名（安全包装）。
     * OSS URL → 带签名的临时链接；外部 URL/null → 原样返回；失败不抛异常。
     */
    default String resolveAvatarUrl(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            return avatarUrl;
        }
        try {
            return resolveAccessibleUrl(avatarUrl);
        } catch (Exception e) {
            return avatarUrl;
        }
    }
}
