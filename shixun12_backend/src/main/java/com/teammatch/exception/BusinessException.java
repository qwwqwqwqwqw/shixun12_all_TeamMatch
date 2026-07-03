package com.teammatch.exception;

import com.teammatch.common.ReasonCode;

/**
 * 业务异常基类
 * 所有业务相关的异常都应继承此类，便于统一处理
 */
public class BusinessException extends RuntimeException {
    
    private final ReasonCode reasonCode;
    
    public BusinessException(ReasonCode reasonCode) {
        super(reasonCode.getMessage());
        this.reasonCode = reasonCode;
    }
    
    public BusinessException(ReasonCode reasonCode, String message) {
        super(message);
        this.reasonCode = reasonCode;
    }
    
    public ReasonCode getReasonCode() {
        return reasonCode;
    }
}
