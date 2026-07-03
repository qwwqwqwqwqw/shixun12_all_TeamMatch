package com.teammatch.controller;

import com.teammatch.common.ReasonCode;
import com.teammatch.common.Result;
import com.teammatch.dto.FileUploadResultVO;
import com.teammatch.exception.ValidationException;
import com.teammatch.service.storage.FileCategory;
import com.teammatch.service.storage.OssService;
import com.teammatch.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 通用文件上传（OSS）
 */
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final OssService ossService;
    private final AuthUtil authUtil;

    /**
     * 上传图片到 OSS
     * POST /api/files/upload?category=avatar|report_evidence|appeal_evidence
     */
    @PostMapping("/upload")
    public Result<FileUploadResultVO> upload(
            @RequestHeader("Authorization") String token,
            @RequestParam("file") MultipartFile file,
            @RequestParam("category") String category) {
        Long userId = authUtil.requireUserId(token);
        try {
            FileCategory fileCategory = parseCategory(category);
            return Result.success(ossService.upload(fileCategory, userId, file));
        } catch (ValidationException e) {
            return Result.of(e.getReasonCode().getCode(), e.getMessage(), null);
        }
    }

    private FileCategory parseCategory(String category) {
        if (category == null) {
            throw new ValidationException(ReasonCode.PARAM_ERROR, "category 不能为空");
        }
        return switch (category.toLowerCase()) {
            case "avatar" -> FileCategory.AVATAR;
            case "report_evidence" -> FileCategory.REPORT_EVIDENCE;
            case "appeal_evidence" -> FileCategory.APPEAL_EVIDENCE;
            default -> throw new ValidationException(ReasonCode.PARAM_ERROR,
                    "category 必须是 avatar、report_evidence 或 appeal_evidence");
        };
    }
}
