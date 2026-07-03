package com.teammatch.exception;

import com.teammatch.common.ReasonCode;

/**
 * 数据未找到异常（如用户不存在、技能标签不存在等）
 */
public class NotFoundException extends BusinessException {
    
    public NotFoundException(ReasonCode reasonCode) {
        super(reasonCode);
    }
    
    public NotFoundException(ReasonCode reasonCode, String message) {
        super(reasonCode, message);
    }
}
