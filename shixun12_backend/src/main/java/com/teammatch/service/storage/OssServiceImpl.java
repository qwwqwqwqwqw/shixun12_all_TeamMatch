package com.teammatch.service.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.teammatch.common.ReasonCode;
import com.teammatch.config.OssProperties;
import com.teammatch.dto.FileUploadResultVO;
import com.teammatch.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OssServiceImpl implements OssService {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final int MAX_EVIDENCE_COUNT = 5;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/jpg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif"
    );

    private static final Map<String, String> CONTENT_TYPE_BY_EXTENSION = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "webp", "image/webp",
            "gif", "image/gif"
    );

    private static final Set<String> GENERIC_CONTENT_TYPES = Set.of(
            "",
            "application/octet-stream",
            "binary/octet-stream",
            "application/x-msdownload"
    );

    private final OssProperties ossProperties;
    private final ObjectProvider<OSS> ossClientProvider;

    @Override
    public FileUploadResultVO upload(FileCategory category, Long userId, MultipartFile file) {
        OSS ossClient = requireOssClient();
        validateFile(file);
        String resolvedContentType = resolveContentType(file);

        String objectKey = buildObjectKey(category, userId, file, resolvedContentType);
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(resolvedContentType);
            ossClient.putObject(
                    ossProperties.getBucket(),
                    objectKey,
                    file.getInputStream(),
                    metadata
            );
        } catch (IOException e) {
            log.error("OSS 上传失败: objectKey={}", objectKey, e);
            throw new ValidationException(ReasonCode.FILE_UPLOAD_FAILED, "文件上传失败");
        }

        String storedUrl = ossProperties.normalizedBaseUrl() + "/" + objectKey;
        String accessUrl = generatePresignedUrl(ossClient, objectKey);
        log.info("OSS 上传成功: userId={}, category={}, objectKey={}", userId, category, objectKey);
        return new FileUploadResultVO(objectKey, storedUrl, accessUrl);
    }

    @Override
    public String resolveAccessibleUrl(String storedUrl) {
        if (storedUrl == null || storedUrl.isBlank()) {
            return storedUrl;
        }
        String normalized = normalizeStoredUrl(storedUrl);
        if (!isOurBucketUrl(normalized)) {
            return storedUrl;
        }
        String objectKey = toObjectKey(normalized);
        if (objectKey == null) {
            return storedUrl;
        }
        return generatePresignedUrl(requireOssClient(), objectKey);
    }

    @Override
    public List<String> resolveAccessibleUrls(List<String> storedUrls) {
        if (storedUrls == null || storedUrls.isEmpty()) {
            return storedUrls;
        }
        return storedUrls.stream().map(this::resolveAccessibleUrl).toList();
    }

    @Override
    public List<String> normalizeStoredUrls(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return urls;
        }
        return urls.stream().map(this::normalizeStoredUrl).toList();
    }

    @Override
    public void validateEvidenceUrls(List<String> evidenceUrls, Long userId, FileCategory category) {
        if (evidenceUrls == null || evidenceUrls.isEmpty()) {
            return;
        }
        if (evidenceUrls.size() > MAX_EVIDENCE_COUNT) {
            throw new ValidationException(ReasonCode.PARAM_ERROR, "证据图片最多上传5张");
        }
        List<String> normalizedUrls = normalizeStoredUrls(evidenceUrls);
        for (String url : normalizedUrls) {
            if (!isOwnedUrl(url, userId, category)) {
                throw new ValidationException(ReasonCode.INVALID_EVIDENCE_URL);
            }
        }
    }

    private OSS requireOssClient() {
        if (!ossProperties.isConfigured()) {
            throw new ValidationException(ReasonCode.OSS_NOT_CONFIGURED);
        }
        OSS ossClient = ossClientProvider.getIfAvailable();
        if (ossClient == null) {
            throw new ValidationException(ReasonCode.OSS_NOT_CONFIGURED);
        }
        return ossClient;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException(ReasonCode.PARAM_ERROR, "上传文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ValidationException(ReasonCode.FILE_SIZE_EXCEEDED);
        }
        resolveContentType(file);
    }

    /**
     * 解析最终 Content-Type：优先使用 multipart 头；Apifox/部分客户端会传 octet-stream，则回退到扩展名。
     */
    private String resolveContentType(MultipartFile file) {
        String contentType = normalizeContentType(file.getContentType());
        if (ALLOWED_CONTENT_TYPES.contains(contentType)) {
            return "image/jpg".equals(contentType) ? "image/jpeg" : contentType;
        }
        if (GENERIC_CONTENT_TYPES.contains(contentType)) {
            String fromExtension = contentTypeFromFilename(file.getOriginalFilename());
            if (fromExtension != null) {
                return fromExtension;
            }
        }
        throw new ValidationException(ReasonCode.FILE_TYPE_NOT_ALLOWED);
    }

    private String buildObjectKey(FileCategory category, Long userId, MultipartFile file, String resolvedContentType) {
        String extension = EXTENSION_BY_CONTENT_TYPE.get(normalizeContentType(resolvedContentType));
        if (extension == null) {
            extension = extensionFromFilename(file.getOriginalFilename());
        }
        if (extension == null) {
            throw new ValidationException(ReasonCode.FILE_TYPE_NOT_ALLOWED);
        }
        String fileName = Instant.now().toEpochMilli() + "_" + UUID.randomUUID().toString().replace("-", "") + "." + extension;
        return category.pathPrefix() + "/" + userId + "/" + fileName;
    }

    private String contentTypeFromFilename(String filename) {
        String extension = extensionFromFilename(filename);
        if (extension == null) {
            return null;
        }
        return CONTENT_TYPE_BY_EXTENSION.get(extension);
    }

    private String extensionFromFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return null;
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String generatePresignedUrl(OSS ossClient, String objectKey) {
        Date expiration = new Date(System.currentTimeMillis()
                + ossProperties.getPresignExpireHours() * 3600L * 1000L);
        URL url = ossClient.generatePresignedUrl(ossProperties.getBucket(), objectKey, expiration);
        return url.toString();
    }

    private boolean isOurBucketUrl(String url) {
        return url != null && url.startsWith(ossProperties.normalizedBaseUrl() + "/");
    }

    private boolean isOwnedUrl(String url, Long userId, FileCategory category) {
        if (!isOurBucketUrl(url)) {
            return false;
        }
        String objectKey = toObjectKey(url);
        if (objectKey == null) {
            return false;
        }
        String expectedPrefix = category.pathPrefix() + "/" + userId + "/";
        return objectKey.startsWith(expectedPrefix);
    }

    private String toObjectKey(String storedUrl) {
        String normalized = normalizeStoredUrl(storedUrl);
        String prefix = ossProperties.normalizedBaseUrl() + "/";
        if (!normalized.startsWith(prefix)) {
            return null;
        }
        return normalized.substring(prefix.length());
    }

    private String normalizeStoredUrl(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }
        int queryIndex = url.indexOf('?');
        if (queryIndex <= 0) {
            return url;
        }
        String withoutQuery = url.substring(0, queryIndex);
        if (isOurBucketUrl(withoutQuery)) {
            return withoutQuery;
        }
        return url;
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        return contentType.toLowerCase(Locale.ROOT).split(";")[0].trim();
    }
}
