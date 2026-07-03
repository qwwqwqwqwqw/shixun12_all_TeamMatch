package com.teammatch.service;

import com.teammatch.common.Result;
import com.teammatch.dto.EvaluationSubmitDTO;
import com.teammatch.dto.EvaluationSubmitResult;

/**
 * M5-5 互评提交最小事务编排服务
 * 串联资格校验、内容校验、信誉计算、异常检测，在同一事务内完成
 */
public interface EvaluationSubmitService {

    /**
     * 提交互评
     * 校验失败返回 Result.fail，落库后失败抛出 BusinessException 触发事务回滚
     */
    Result<EvaluationSubmitResult> submit(EvaluationSubmitDTO dto);
}
