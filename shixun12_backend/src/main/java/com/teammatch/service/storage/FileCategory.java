package com.teammatch.service.storage;

/**
 * OSS 上传文件分类
 */
public enum FileCategory {
    AVATAR("avatars"),
    REPORT_EVIDENCE("evidences/reports"),
    APPEAL_EVIDENCE("evidences/appeals");

    private final String pathPrefix;

    FileCategory(String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }

    public String pathPrefix() {
        return pathPrefix;
    }
}
