package com.teammatch.common;

import java.util.HashMap;
import java.util.Map;

/**
 * M5 模块错误码枚举
 * 用于统一管理互评业务的错误码和错误信息
 */
public enum ReasonCode {
    SUCCESS("00000", "成功"),

    // 通用错误（M10xx）
    PARAM_ERROR("M1001", "参数错误"),
    NOT_FOUND("M1002", "资源不存在"),
    STATUS_CONFLICT("M1003", "状态冲突"),
    FORBIDDEN("M1004", "无权访问该资源"),

    // M3 认证与档案错误（M30xx）
    UNAUTHORIZED("M3000", "未授权，请重新登录"),
    FORMAL_PROFILE_REQUIRED("M3001", "需要先完成正式档案"),
    EMAIL_OCCUPIED("M3002", "该邮箱已被其他用户绑定"),
    INVALID_CODE("M3003", "验证码错误或已过期"),
    SEND_CODE_TOO_FREQUENT("M3004", "发送验证码过于频繁"),
    INVALID_EMAIL("M3023", "邮箱格式不正确"),
    EMAIL_SEND_FAILED("M3024", "邮件发送失败，请稍后重试"),
    EMAIL_ALREADY_VERIFIED("M3028", "该邮箱已认证，无需重复绑定"),
    GITHUB_USER_NOT_FOUND("M3029", "GitHub 用户不存在"),
    GITHUB_ALREADY_BOUND("M3030", "已绑定 GitHub 账号，请使用更新接口"),
    USERNAME_OCCUPIED("M3005", "该用户名已被占用"),
    USERNAME_ALREADY_BOUND("M3006", "用户名已绑定，请使用修改接口"),
    PASSWORD_TOO_SHORT("M3007", "密码长度不能少于6位"),
    OLD_PASSWORD_INCORRECT("M3008", "旧密码不正确"),
    ADMIN_REQUIRED("M3009", "需要管理员权限"),
    SKILL_TAG_NOT_FOUND("M3010", "技能标签不存在或已禁用"),
    USERNAME_REQUIRED("M3011", "用户名不能为空"),
    WECHAT_LOGIN_FAILED("M3025", "微信登录失败，请重试"),
    GITHUB_NOT_BOUND("M3012", "GitHub 账号未绑定，请先使用绑定接口"),
    GITEE_USER_NOT_FOUND("M3031", "Gitee 用户不存在"),
    GITEE_ALREADY_BOUND("M3032", "已绑定 Gitee 账号，请使用更新接口"),
    GITEE_NOT_BOUND("M3033", "Gitee 账号未绑定，请先使用绑定接口"),
    INVALID_PASSWORD("M3013", "用户名或密码错误"),
    PASSWORD_NOT_SET("M3014", "该用户未设置密码，请先联系管理员创建密码"),
    SKILL_TAG_ALREADY_EXISTS("M3015", "技能标签已存在"),
    PASSWORD_REQUIRED("M3016", "密码不能为空"),
    ACCOUNT_BANNED("M3017", "账号已被封禁"),
    USER_NOT_FOUND("M3018", "用户不存在"),
    PASSWORD_ALREADY_SET("M3019", "该用户已设置密码"),
    OLD_PASSWORD_REQUIRED("M3020", "旧密码不能为空"),
    SAME_OLD_NEW_PASSWORD("M3021", "新密码不能与旧密码相同"),
    SKILL_TAG_NAME_REQUIRED("M3022", "技能标签名称不能为空"),
    TECH_PROFILE_NOT_FOUND("M3026", "技术画像不存在，请先绑定 GitHub 账号"),
    TECH_PROFILE_ALREADY_CLAIMED("M3027", "该 GitHub 账号已被其他用户认领"),

    // 互评资格错误
    NOT_PROJECT_MEMBER("NOT_PROJECT_MEMBER", "当前用户不是该项目成员"),
    PROJECT_NOT_ENDED("PROJECT_NOT_ENDED", "项目尚未进入互评阶段"),
    EVAL_WINDOW_CLOSED("EVAL_WINDOW_CLOSED", "互评窗口已关闭"),
    PROJECT_NOT_FOUND("M5004", "项目不存在"),

    // 互评成员错误
    SELF_EVALUATION("SELF_EVALUATION", "不能评价自己"),
    ALREADY_EVALUATED("ALREADY_EVALUATED", "已评价过该成员"),
    TARGET_NOT_PROJECT_MEMBER("TARGET_NOT_PROJECT_MEMBER", "目标用户不是项目成员"),

    // 内容校验级错误（M52xx）
    SCORE_FIELD_MISSING("M5201", "评分字段缺失"),
    SCORE_OUT_OF_RANGE("M5202", "评分超出范围"),
    COMMENT_CONTAINS_VIOLATION("M5203", "评论包含违规词"),
    HIGH_SCORE_MISSING_POSITIVE_TAG("M5204", "高分缺少正向标签"),
    LOW_SCORE_MISSING_NEGATIVE_TAG("M5205", "低分缺少负向标签"),
    LOW_SCORE_COMMENT_TOO_SHORT("M5206", "低分评论过短"),

    // 评价复核错误（M50xx）
    EVALUATION_NOT_FOUND("M5005", "评价记录不存在"),
    CREDIT_CHANGE_NOT_FOUND("M5006", "信誉分流水不存在或状态异常"),
    INVALID_REVIEW_ACTION("M5007", "无效的复核操作"),
    REVIEW_NOTE_TOO_LONG("M5008", "复核备注过长"),

    // M4 组队/退出错误（M40xx）
    M4_PROJECT_NOT_FOUND("M4001", "项目不存在"),
    M4_NOT_LEADER("M4002", "无队长权限"),
    M4_PROJECT_STATUS_INVALID("M4003", "项目状态不符合要求"),
    M4_MEMBER_NOT_ACTIVE("M4004", "该用户不是项目的活跃成员"),
    M4_RESOURCE_NOT_FOUND("M4005", "请求或投票不存在"),
    M4_DUPLICATE_OPERATION("M4006", "重复操作"),
    M4_TEAM_FULL("M4007", "团队成员已达上限"),
    M4_VOTE_ALREADY_CLOSED("M4008", "投票已结束"),
    M4_LEADER_CANNOT_EXIT("M4009", "队长不能退出，请先转让队长身份"),
    M4_CANNOT_VOTE_LEADER("M4010", "不能对队长发起退出投票"),
    M4_CANNOT_VOTE_SELF("M4011", "目标成员不能参与自己的退出投票"),
    M4_DUPLICATE_PENDING_REQUEST("M4012", "已有待处理的请求"),
    M4_USER_ALREADY_IN_PROJECT("M4013", "用户已在项目中"),
    M4_REQUEST_ALREADY_HANDLED("M4014", "该请求已处理"),
    M4_UNAUTHORIZED_REQUEST("M4015", "无权操作此请求"),
    INVALID_PENALTY_LEVEL("INVALID_PENALTY_LEVEL", "处罚级别缺失或不合法"),
    M4_TEAM_VOTE_CONFLICT("TEAM_VOTE_CONFLICT", "退出操作冲突，请重试"),

    // M6 治理错误（M60xx）
    PENALTY_SELF_NOT_ALLOWED("M6001", "不能对自己执行处罚"),
    FILE_UPLOAD_FAILED("M6020", "文件上传失败"),
    FILE_TYPE_NOT_ALLOWED("M6021", "不支持的文件类型，仅允许 JPG/PNG/WEBP/GIF"),
    FILE_SIZE_EXCEEDED("M6022", "文件大小不能超过5MB"),
    INVALID_EVIDENCE_URL("M6023", "证据图片URL无效，请先通过上传接口获取"),
    OSS_NOT_CONFIGURED("M6024", "文件存储未配置，请联系管理员"),

    // 申诉恢复错误（M50xx）
    APPEAL_NOT_FOUND("M5009", "申诉记录不存在"),
    APPEAL_NOT_APPROVED("M5010", "申诉尚未通过审批"),
    INVALID_APPEAL_TARGET_TYPE("M5011", "申诉目标类型不是评价"),

    // 评价无效（英文常量名，与 ALREADY_EVALUATED 等一致）
    EVALUATION_ALREADY_INVALIDATED("EVALUATION_ALREADY_INVALIDATED", "评价已被判定无效"),

    // 通用错误
    UNKNOWN_ERROR("M9999", "未知错误");

    private final String code;
    private final String message;

    /** 错误码到枚举的映射缓存，提升查找性能 */
    private static final Map<String, ReasonCode> CODE_MAP = new HashMap<>();

    static {
        // 类加载时初始化映射表
        for (ReasonCode reasonCode : ReasonCode.values()) {
            CODE_MAP.put(reasonCode.code, reasonCode);
        }
    }

    ReasonCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    /** 获取错误码 */
    public String getCode() {
        return code;
    }

    /** 获取错误信息 */
    public String getMessage() {
        return message;
    }

    /**
     * 根据错误码字符串查找对应的枚举
     * 使用 Map 缓存，时间复杂度 O(1)
     *
     * @param code 错误码字符串（如 "NOT_PROJECT_MEMBER"）
     * @return 对应的 ReasonCode 枚举，找不到返回 UNKNOWN_ERROR
     */
    public static ReasonCode fromCode(String code) {
        return CODE_MAP.getOrDefault(code, UNKNOWN_ERROR);
    }
}
