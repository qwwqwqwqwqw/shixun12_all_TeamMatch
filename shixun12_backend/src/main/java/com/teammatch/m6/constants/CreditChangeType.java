package com.teammatch.m6.constants;

/**
 * {@code credit_change.change_type} 冻结枚举（V2.1 §6.9）。
 * <p>
 * M6 处罚链仅写入 {@link #PENALTY}（扣分）与 {@link #PENALTY_RESTORE}（撤销恢复）。
 * 表唯一键 {@code uk_credit_change_source_user_type(source_type, source_id, user_id, change_type)}
 * 要求同一处罚扣分与恢复必须使用不同 change_type，故恢复不能复用 {@code penalty}。
 * <p>
 * 完整冻结清单还包含 M4/M5 使用的：evaluation、exit_vote、self_exit、appeal_restore。
 */
public final class CreditChangeType {

    /** 管理员执行 credit_deduct 处罚时的扣分流水（change_value 为负） */
    public static final String PENALTY = "penalty";

    /**
     * 管理员撤销 credit_deduct 处罚时的恢复流水（change_value 为正）。
     * 与 {@link #PENALTY} 对称；区别于 M5 的 appeal_restore（互评申诉恢复）。
     */
    public static final String PENALTY_RESTORE = "penalty_restore";

    public static final String SOURCE_TYPE_PENALTY = "penalty";

    private CreditChangeType() {
    }
}
