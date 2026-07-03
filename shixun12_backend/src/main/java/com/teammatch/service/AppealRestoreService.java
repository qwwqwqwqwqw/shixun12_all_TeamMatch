package com.teammatch.service;

import com.teammatch.common.Result;
import com.teammatch.dto.AppealRestoreCommand;
import com.teammatch.dto.AppealRestoreResult;

/**
 * M5-7 申诉恢复 Service
 * 仅处理 appeal(target_type='evaluation') 的信誉恢复
 * function_limit 申诉恢复由 M6 单独处理，不调用本服务
 */
public interface AppealRestoreService {

    /**
     * 对已批准的 evaluation 类申诉执行信誉恢复
     *
     * @param command 入参，含 appealId
     * @return 恢复结果：成功 / skipped（原流水未生效）/ alreadyRestored（幂等）
     */
    Result<AppealRestoreResult> restore(AppealRestoreCommand command);
}
