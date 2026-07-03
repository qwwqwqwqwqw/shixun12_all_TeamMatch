package com.teammatch.controller;

import com.teammatch.common.ReasonCode;
import com.teammatch.common.Result;
import com.teammatch.dto.FileUploadResultVO;
import com.teammatch.exception.ValidationException;
import com.teammatch.service.storage.FileCategory;
import com.teammatch.service.storage.OssService;
import com.teammatch.util.AuthUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("文件上传控制器测试")
class FileUploadControllerTest {

    @Mock
    private OssService ossService;

    @Mock
    private AuthUtil authUtil;

    @InjectMocks
    private FileUploadController controller;

    private static final String TOKEN = "Bearer test-token";
    private static final Long USER_ID = 100L;

    @BeforeEach
    void setUp() {
        when(authUtil.requireUserId(TOKEN)).thenReturn(USER_ID);
    }

    @Test
    @DisplayName("upload: 头像上传成功")
    void upload_success() {
        FileUploadResultVO uploadResult = new FileUploadResultVO(
                "avatars/100/test.jpg",
                "https://teammatch.oss-cn-beijing.aliyuncs.com/avatars/100/test.jpg",
                "https://teammatch.oss-cn-beijing.aliyuncs.com/avatars/100/test.jpg?signature=xxx"
        );
        when(ossService.upload(eq(FileCategory.AVATAR), eq(USER_ID), any()))
                .thenReturn(uploadResult);

        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.jpg", "image/jpeg", "fake-image".getBytes());

        Result<FileUploadResultVO> result = controller.upload(TOKEN, file, "avatar");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getStoredUrl()).isEqualTo(uploadResult.getStoredUrl());
    }

    @Test
    @DisplayName("upload: 非法 category 返回参数错误")
    void upload_invalidCategory() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.jpg", "image/jpeg", "fake-image".getBytes());

        Result<FileUploadResultVO> result = controller.upload(TOKEN, file, "invalid");

        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.PARAM_ERROR.getCode());
    }

    @Test
    @DisplayName("upload: OSS 未配置")
    void upload_ossNotConfigured() {
        when(ossService.upload(any(), any(), any()))
                .thenThrow(new ValidationException(ReasonCode.OSS_NOT_CONFIGURED));

        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.jpg", "image/jpeg", "fake-image".getBytes());

        Result<FileUploadResultVO> result = controller.upload(TOKEN, file, "avatar");

        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.OSS_NOT_CONFIGURED.getCode());
    }
}
