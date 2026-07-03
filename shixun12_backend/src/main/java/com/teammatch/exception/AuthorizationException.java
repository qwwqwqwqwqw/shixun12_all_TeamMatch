package com.teammatch.exception;

import com.teammatch.common.ReasonCode;

/**
 * 权限相关异常
 */
public class AuthorizationException extends BusinessException {
    
    public AuthorizationException(ReasonCode reasonCode) {
        super(reasonCode);
    }
    
    public AuthorizationException(ReasonCode reasonCode, String message) {
        super(reasonCode, message);
    }
}
