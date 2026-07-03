package com.teammatch.exception;

import com.teammatch.common.ReasonCode;

/**
 * 数据重复异常（如用户名、邮箱、技能标签等已存在）
 */
public class DuplicateDataException extends BusinessException {
    
    public DuplicateDataException(ReasonCode reasonCode) {
        super(reasonCode);
    }
    
    public DuplicateDataException(ReasonCode reasonCode, String message) {
        super(reasonCode, message);
    }
}
