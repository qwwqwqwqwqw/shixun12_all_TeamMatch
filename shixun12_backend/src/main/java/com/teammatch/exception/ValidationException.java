package com.teammatch.exception;

import com.teammatch.common.ReasonCode;

/**
 * 参数校验异常（如密码长度不足、字段为空等）
 */
public class ValidationException extends BusinessException {
    
    public ValidationException(ReasonCode reasonCode) {
        super(reasonCode);
    }
    
    public ValidationException(ReasonCode reasonCode, String message) {
        super(reasonCode, message);
    }
}
