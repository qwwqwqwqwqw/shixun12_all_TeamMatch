package com.teammatch.common;

import java.util.HashMap;
import java.util.Map;

/**
 * 评价复核操作枚举
 * 用于 M5-6 评价复核 Service 的操作类型校验
 */
public enum ReviewAction {
    APPROVE("approve"),
    VOID("void"),
    KEEP_NO_CREDIT("keep_no_credit");

    private final String value;

    private static final Map<String, ReviewAction> MAP = new HashMap<>();

    static {
        for (ReviewAction action : ReviewAction.values()) {
            MAP.put(action.value, action);
        }
    }

    ReviewAction(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ReviewAction fromString(String action) {
        if (action == null) {
            return null;
        }
        return MAP.get(action);
    }
}
