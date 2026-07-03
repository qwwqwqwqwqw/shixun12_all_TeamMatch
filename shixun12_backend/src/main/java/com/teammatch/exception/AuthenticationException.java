package com.teammatch.exception;

import com.teammatch.common.ReasonCode;

/**
 * 认证相关异常
 */
public class AuthenticationException extends BusinessException {
    
    public AuthenticationException(ReasonCode reasonCode) {
        super(reasonCode);
    }
    
    public AuthenticationException(ReasonCode reasonCode, String message) {
        super(reasonCode, message);
    }
}
