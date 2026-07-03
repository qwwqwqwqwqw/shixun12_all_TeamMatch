package com.teammatch.service.storage;

import com.aliyun.oss.OSS;
import com.teammatch.common.ReasonCode;
import com.teammatch.config.OssProperties;
import com.teammatch.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockMultipartFile;

import java.net.URL;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OSS 服务单元测试")
class OssServiceImplTest {

    @Mock
    private OSS ossClient;

    @Mock
    private ObjectProvider<OSS> ossClientProvider;

    private OssProperties ossProperties;
    private OssServiceImpl ossService;

    @BeforeEach
    void setUp() {
        ossProperties = new OssProperties();
        ossProperties.setEndpoint("oss-cn-beijing.aliyuncs.com");
        ossProperties.setAccessKeyId("test-key-id");
        ossProperties.setAccessKeySecret("test-key-secret");
        ossProperties.setBucket("teammatch");
        ossProperties.setBaseUrl("https://teammatch.oss-cn-beijing.aliyuncs.com");
        ossProperties.setPresignExpireHours(24);

        lenient().when(ossClientProvider.getIfAvailable()).thenReturn(ossClient);
        ossService = new OssServiceImpl(ossProperties, ossClientProvider);
    }

    @Test
    @DisplayName("validateEvidenceUrls: 合法 URL 通过")
    void validateEvidenceUrls_success() {
        String url = "https://teammatch.oss-cn-beijing.aliyuncs.com/evidences/reports/100/a.jpg";
        ossService.validateEvidenceUrls(List.of(url), 100L, FileCategory.REPORT_EVIDENCE);
    }

    @Test
    @DisplayName("validateEvidenceUrls: 非本 Bucket URL 拒绝")
    void validateEvidenceUrls_invalidUrl() {
        assertThatThrownBy(() -> ossService.validateEvidenceUrls(
                List.of("https://evil.example.com/a.jpg"), 100L, FileCategory.REPORT_EVIDENCE))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getReasonCode())
                        .isEqualTo(ReasonCode.INVALID_EVIDENCE_URL));
    }

    @Test
    @DisplayName("validateEvidenceUrls: 超过5张拒绝")
    void validateEvidenceUrls_tooMany() {
        String url = "https://teammatch.oss-cn-beijing.aliyuncs.com/evidences/reports/100/a.jpg";
        assertThatThrownBy(() -> ossService.validateEvidenceUrls(
                List.of(url, url, url, url, url, url), 100L, FileCategory.REPORT_EVIDENCE))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("upload: 空文件拒绝")
    void upload_emptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[0]);
        assertThatThrownBy(() -> ossService.upload(FileCategory.AVATAR, 1L, file))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("upload: 非法类型拒绝")
    void upload_invalidType() {
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "hello".getBytes());
        assertThatThrownBy(() -> ossService.upload(FileCategory.AVATAR, 1L, file))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getReasonCode())
                        .isEqualTo(ReasonCode.FILE_TYPE_NOT_ALLOWED));
    }

    @Test
    @DisplayName("upload: octet-stream + .png 扩展名可通过")
    void upload_octetStreamWithPngExtension() throws Exception {
        when(ossClient.generatePresignedUrl(anyString(), anyString(), any()))
                .thenReturn(new URL("https://teammatch.oss-cn-beijing.aliyuncs.com/avatars/1/test.png?sig=1"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "logo.png", "application/octet-stream", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});

        var result = ossService.upload(FileCategory.AVATAR, 1L, file);
        assertThat(result.getStoredUrl()).contains(".png");
        assertThat(result.getAccessUrl()).contains("sig=1");
    }

    @Test
    @DisplayName("normalizeStoredUrls: accessUrl 去掉 query 后可校验通过")
    void validateEvidenceUrls_acceptsAccessUrlWithQuery() {
        String stored = "https://teammatch.oss-cn-beijing.aliyuncs.com/evidences/reports/100/a.png";
        String access = stored + "?Expires=123&Signature=abc";
        ossService.validateEvidenceUrls(List.of(access), 100L, FileCategory.REPORT_EVIDENCE);
    }

    @Test
    @DisplayName("resolveAccessibleUrls: 空列表原样返回")
    void resolveAccessibleUrls_empty() {
        assertThat(ossService.resolveAccessibleUrls(null)).isNull();
        assertThat(ossService.resolveAccessibleUrls(List.of())).isEmpty();
    }

    @Test
    @DisplayName("resolveAccessibleUrl: 外部 URL 原样返回")
    void resolveAccessibleUrl_external() {
        String external = "https://avatars.githubusercontent.com/u/1";
        assertThat(ossService.resolveAccessibleUrl(external)).isEqualTo(external);
    }

    @Test
    @DisplayName("resolveAccessibleUrl: 本 Bucket URL 生成 presign")
    void resolveAccessibleUrl_ossUrl() throws Exception {
        String stored = "https://teammatch.oss-cn-beijing.aliyuncs.com/avatars/1/a.jpg";
        when(ossClient.generatePresignedUrl(anyString(), anyString(), any()))
                .thenReturn(new URL(stored + "?sig=1"));
        assertThat(ossService.resolveAccessibleUrl(stored)).contains("sig=1");
    }

    @Test
    @DisplayName("normalizeStoredUrls: 去掉 query 参数")
    void normalizeStoredUrls_stripQuery() {
        String stored = "https://teammatch.oss-cn-beijing.aliyuncs.com/evidences/reports/1/a.png";
        assertThat(ossService.normalizeStoredUrls(List.of(stored + "?sig=1")))
                .containsExactly(stored);
    }

    @Test
    @DisplayName("upload: 超过 5MB 拒绝")
    void upload_fileTooLarge() {
        byte[] large = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", large);
        assertThatThrownBy(() -> ossService.upload(FileCategory.AVATAR, 1L, file))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getReasonCode())
                        .isEqualTo(ReasonCode.FILE_SIZE_EXCEEDED));
    }

    @Test
    @DisplayName("upload: OSS 未配置")
    void upload_ossNotConfigured() {
        when(ossClientProvider.getIfAvailable()).thenReturn(null);
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", "x".getBytes());
        assertThatThrownBy(() -> ossService.upload(FileCategory.AVATAR, 1L, file))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getReasonCode())
                        .isEqualTo(ReasonCode.OSS_NOT_CONFIGURED));
    }
}
