package com.teammatch.m4.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.teammatch.m4.dto.ExitVoteCreateDTO;
import com.teammatch.m4.dto.ExitVoteSubmitDTO;
import com.teammatch.m4.dto.ExitVoteVO;
import com.teammatch.m4.entity.ExitVote;

import java.util.List;

/**
 * T-136~T-141: 退出投票服务接口
 */
public interface ExitVoteService extends IService<ExitVote> {

    /** T-136: 成员主动退出 */
    void selfExit(Long projectId, Long userId);

    /** T-137: 队长发起退出投票 */
    ExitVoteVO initiateVote(Long projectId, ExitVoteCreateDTO dto);

    /** T-138: 投票详情 */
    ExitVoteVO getVoteDetail(Long voteId);

    /** T-139: 提交投票（agree/disagree），防重复 */
    void submitVote(Long voteId, ExitVoteSubmitDTO dto);

    /** T-140: 关闭投票并执行结果（加事务+幂等） */
    ExitVoteVO closeVote(Long voteId, Long operatorId);

    /** T-145: 队长取消进行中的投票 */
    void cancelVote(Long voteId, Long operatorId);

    /** 查询项目下所有退出投票列表（按创建时间倒序） */
    List<ExitVoteVO> getVoteList(Long projectId);
}
