package com.teammatch.m4.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.teammatch.entity.CreditChange;
import com.teammatch.m4.dto.ExitVoteCreateDTO;
import com.teammatch.m4.dto.ExitVoteSubmitDTO;
import com.teammatch.m4.dto.ExitVoteVO;
import com.teammatch.m4.entity.ExitVote;
import com.teammatch.m4.entity.ExitVoteRecord;
import com.teammatch.m4.entity.TeamMember;
import com.teammatch.m4.mapper.M4ExitVoteMapper;
import com.teammatch.m4.mapper.M4ExitVoteRecordMapper;
import com.teammatch.m4.mapper.M4TeamMemberMapper;
import com.teammatch.m4.entity.Project;
import com.teammatch.m4.service.ExitVoteService;
import com.teammatch.m4.service.ProjectService;
import com.teammatch.m4.service.TeamMemberService;
import com.teammatch.mapper.CreditChangeMapper;
import com.teammatch.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * T-136~T-141: 退出流程实现
 */
@Service
@RequiredArgsConstructor
public class ExitVoteServiceImpl extends ServiceImpl<M4ExitVoteMapper, ExitVote> implements ExitVoteService {

    private final M4ExitVoteRecordMapper exitVoteRecordMapper;
    private final TeamMemberService teamMemberService;
    private final ProjectService projectService;
    private final CreditChangeMapper creditChangeMapper;
    private final UserMapper userMapper;
    private final M4TeamMemberMapper teamMemberMapper;

    // ------------------------------------------------------------------ T-136
    /**
     * 成员主动退出（V2.1）：
     * - recruiting 阶段：仅更改成员状态，不写信誉流水
     * - in_progress 阶段：CAS 关闭投票 → CAS 更新成员 → 写扣分流水
     * 统一锁顺序 exit_vote → team_member，与 closeVote/lazyClose 一致，避免死锁。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void selfExit(Long projectId, Long userId) {
        TeamMember member = getActiveMember(projectId, userId);
        if (member == null) {
            throw new RuntimeException("该用户不是项目的活跃成员");
        }
        if ("leader".equals(member.getRole())) {
            throw new RuntimeException("队长不能主动退出，请先转让队长身份");
        }
        Project project = projectService.getById(projectId);
        if (project == null) {
            throw new RuntimeException("项目不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        if ("in_progress".equals(project.getStatus())) {
            // 第一步：CAS 关闭该用户进行中的投票（先锁 exit_vote，与 closeVote/lazyClose 统一顺序）
            List<ExitVote> activeVotes = this.list(new LambdaQueryWrapper<ExitVote>()
                    .eq(ExitVote::getProjectId, projectId)
                    .eq(ExitVote::getTargetUserId, userId)
                    .eq(ExitVote::getStatus, "voting"));
            for (ExitVote v : activeVotes) {
                baseMapper.update(null, new UpdateWrapper<ExitVote>()
                        .eq("id", v.getId())
                        .eq("status", "voting")
                        .set("status", "closed")
                        .set("closed_at", now)
                        .set("updated_at", now));
                // rows == 0 表示 closeVote/lazyClose 已抢占，跳过即可
            }

            // 第二步：CAS 更新成员状态（再锁 team_member）
            int memberRows = teamMemberMapper.update(null, new UpdateWrapper<TeamMember>()
                    .eq("id", member.getId())
                    .eq("status", "active")
                    .set("status", "exited")
                    .set("exit_mode", "self_exit")
                    .set("left_at", now)
                    .set("updated_at", now));
            if (memberRows != 1) {
                throw new RuntimeException("TEAM_VOTE_CONFLICT");
            }

            // 第三步：CAS 成功才写扣分
            writeCreditChange(userId, projectId, "self_exit", "team_member", member.getId(),
                    "主动退出项目，信誉分扣 10 分", -10);
        } else {
            // recruiting 阶段：轻量退出，不写信誉流水
            int memberRows = teamMemberMapper.update(null, new UpdateWrapper<TeamMember>()
                    .eq("id", member.getId())
                    .eq("status", "active")
                    .set("status", "exited")
                    .set("exit_mode", "self_exit")
                    .set("left_at", now)
                    .set("updated_at", now));
            if (memberRows != 1) {
                throw new RuntimeException("TEAM_VOTE_CONFLICT");
            }
        }
    }

    // ------------------------------------------------------------------ T-137
    /**
     * 队长发起退出投票：
     * 1. 项目必须是 in_progress
     * 2. 操作人必须是队长
     * 3. 目标成员必须是 active 且不是队长
     * 4. 同一项目同一目标不能存在 voting 状态的投票（防重复）
     * 5. 快照当前参与投票人数（所有 active 成员，不含目标成员自身）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExitVoteVO initiateVote(Long projectId, ExitVoteCreateDTO dto) {
        // 校验项目必须是 in_progress
        Project project = projectService.getById(projectId);
        if (project == null) {
            throw new RuntimeException("项目不存在");
        }
        if (!"in_progress".equals(project.getStatus())) {
            throw new RuntimeException("只有进行中的项目才能发起退出投票");
        }
        // 校验发起人是队长
        TeamMember initiator = getActiveMember(projectId, dto.getInitiatorId());
        if (initiator == null || !"leader".equals(initiator.getRole())) {
            throw new RuntimeException("只有队长可以发起退出投票");
        }
        // 校验目标成员
        TeamMember target = getActiveMember(projectId, dto.getTargetUserId());
        if (target == null) {
            throw new RuntimeException("目标成员不是项目活跃成员");
        }
        if ("leader".equals(target.getRole())) {
            throw new RuntimeException("不能对队长发起退出投票");
        }
        // 防重复投票（唯一活跃投票由 active_vote_key 约束，此处做业务校验）
        long votingCount = this.count(new LambdaQueryWrapper<ExitVote>()
                .eq(ExitVote::getProjectId, projectId)
                .eq(ExitVote::getTargetUserId, dto.getTargetUserId())
                .eq(ExitVote::getStatus, "voting"));
        if (votingCount > 0) {
            throw new RuntimeException("该成员已有进行中的退出投票");
        }
        // 校验 penaltyLevel 合法性（V2.1 仅允许 negotiated / malicious，缺失或非法均拒绝）
        String penaltyLevel = dto.getPenaltyLevel();
        if (!"negotiated".equals(penaltyLevel) && !"malicious".equals(penaltyLevel)) {
            throw new RuntimeException("INVALID_PENALTY_LEVEL");
        }
        // 快照可投票人数（不含目标成员）
        long voterCount = teamMemberService.count(new LambdaQueryWrapper<TeamMember>()
                .eq(TeamMember::getProjectId, projectId)
                .eq(TeamMember::getStatus, "active")
                .ne(TeamMember::getUserId, dto.getTargetUserId()));

        ExitVote vote = new ExitVote();
        vote.setProjectId(projectId);
        vote.setInitiatorId(dto.getInitiatorId());
        vote.setTargetUserId(dto.getTargetUserId());
        vote.setReason(dto.getReason());
        vote.setPenaltyLevel(penaltyLevel);
        vote.setStatus("voting");
        vote.setAgreeCount(0);
        vote.setDisagreeCount(0);
        vote.setTotalVoters((int) voterCount);
        vote.setDeadlineAt(LocalDateTime.now().plusHours(24));
        vote.setCreatedAt(LocalDateTime.now());
        vote.setUpdatedAt(LocalDateTime.now());
        this.save(vote);
        return toVO(vote);
    }

    // ------------------------------------------------------------------ T-138
    /** 返回投票详情（含统计），懒检查 deadline */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExitVoteVO getVoteDetail(Long voteId) {
        ExitVote vote = this.getById(voteId);
        if (vote == null) {
            throw new RuntimeException("投票不存在");
        }
        lazyCloseIfDeadlinePassed(vote);
        return toVO(vote);
    }

    // ------------------------------------------------------------------ T-139
    /**
     * 提交投票（agree/disagree）：
     * 1. 投票必须是 open
     * 2. 投票人必须是活跃成员且不是目标成员
     * 3. 不能重复投票（幂等）
     * 4. 更新计数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitVote(Long voteId, ExitVoteSubmitDTO dto) {
        ExitVote vote = this.getById(voteId);
        if (vote == null) {
            throw new RuntimeException("投票不存在");
        }
        lazyCloseIfDeadlinePassed(vote);
        if (!"voting".equals(vote.getStatus())) {
            throw new RuntimeException("投票已关闭");
        }
        if (vote.getTargetUserId().equals(dto.getVoterId())) {
            throw new RuntimeException("目标成员不能参与自己的退出投票");
        }
        // 校验投票人必须是项目 active 成员
        TeamMember voter = getActiveMember(vote.getProjectId(), dto.getVoterId());
        if (voter == null) {
            throw new RuntimeException("投票人不是项目活跃成员");
        }
        // 防重复投票（T-141 幂等）
        long alreadyVoted = exitVoteRecordMapper.selectCount(new LambdaQueryWrapper<ExitVoteRecord>()
                .eq(ExitVoteRecord::getVoteId, voteId)
                .eq(ExitVoteRecord::getVoterId, dto.getVoterId()));
        if (alreadyVoted > 0) {
            throw new RuntimeException("DUPLICATE_VOTE");
        }
        // 写投票记录
        ExitVoteRecord record = new ExitVoteRecord();
        record.setVoteId(voteId);
        record.setVoterId(dto.getVoterId());
        record.setChoice(dto.getChoice());
        record.setCreatedAt(LocalDateTime.now());
        exitVoteRecordMapper.insert(record);

        // 更新计数
        if ("agree".equals(dto.getChoice())) {
            vote.setAgreeCount(vote.getAgreeCount() + 1);
        } else {
            vote.setDisagreeCount(vote.getDisagreeCount() + 1);
        }
        vote.setUpdatedAt(LocalDateTime.now());
        this.updateById(vote);
    }

    // ------------------------------------------------------------------ T-140 / T-141
    /**
     * 关闭投票并执行结果（T-141：事务 + 幂等）：
     * 通过 selectByIdForUpdate 持有行锁，避免并发双重执行副作用。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExitVoteVO closeVote(Long voteId, Long operatorId) {
        ExitVote vote = baseMapper.selectByIdForUpdate(voteId);
        if (vote == null) {
            throw new RuntimeException("投票不存在");
        }
        if (!"voting".equals(vote.getStatus())) {
            return toVO(vote); // T-141 幂等：已关闭直接返回
        }
        if (!vote.getInitiatorId().equals(operatorId)) {
            throw new RuntimeException("只有发起人可以关闭投票");
        }
        doCloseVote(vote);
        return toVO(vote);
    }

    // ------------------------------------------------------------------ T-145: 取消投票

    /**
     * 队长取消进行中的投票。
     * 只有发起人（队长）可取消，且投票必须是 voting 状态。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelVote(Long voteId, Long operatorId) {
        ExitVote vote = baseMapper.selectByIdForUpdate(voteId);
        if (vote == null) {
            throw new RuntimeException("投票不存在");
        }
        if (!"voting".equals(vote.getStatus())) {
            throw new RuntimeException("投票已关闭");
        }
        if (!vote.getInitiatorId().equals(operatorId)) {
            throw new RuntimeException("只有发起人可以取消投票");
        }
        LocalDateTime now = LocalDateTime.now();
        vote.setStatus("cancelled");
        vote.setResult(null);
        vote.setClosedAt(now);
        vote.setUpdatedAt(now);
        this.updateById(vote);
    }

    // ------------------------------------------------------------------ 投票列表

    /**
     * 查询项目下所有退出投票列表（按创建时间倒序）。
     */
    @Override
    public List<ExitVoteVO> getVoteList(Long projectId) {
        return this.list(new LambdaQueryWrapper<ExitVote>()
                .eq(ExitVote::getProjectId, projectId)
                .orderByDesc(ExitVote::getCreatedAt))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    // ------------------------------------------------------------------ helper: close vote core

    /**
     * 执行投票关闭核心逻辑（调用方须已持有行锁或通过 CAS 确认独占写权）。
     */
    private void doCloseVote(ExitVote vote) {
        boolean pass = vote.getAgreeCount() > vote.getDisagreeCount();
        vote.setStatus("closed");
        vote.setResult(pass ? "pass" : "reject");
        vote.setClosedAt(LocalDateTime.now());
        vote.setUpdatedAt(LocalDateTime.now());
        this.updateById(vote);
        if (pass) {
            executePassedExitSideEffects(vote);
        }
    }

    /**
     * pass 时的副作用：CAS 踢出成员 + 按 penaltyLevel 写信誉流水。
     * negotiated=-5，malicious=-10。CAS 失败（memberRows==0）表示 selfExit 已抢占，跳过扣分。
     */
    private void executePassedExitSideEffects(ExitVote vote) {
        LocalDateTime now = LocalDateTime.now();
        int memberRows = teamMemberMapper.update(null, new UpdateWrapper<TeamMember>()
                .eq("project_id", vote.getProjectId())
                .eq("user_id", vote.getTargetUserId())
                .eq("status", "active")
                .set("status", "exited")
                .set("exit_mode", "exit_vote")
                .set("left_at", now)
                .set("updated_at", now));
        if (memberRows == 0) {
            return; // selfExit 已抢占成员状态，不产生额外扣分流水
        }
        int changeValue = "malicious".equals(vote.getPenaltyLevel()) ? -10 : -5;
        writeCreditChange(vote.getTargetUserId(), vote.getProjectId(),
                "exit_vote", "exit_vote", vote.getId(),
                "被投票退出项目，信誉分扣 " + Math.abs(changeValue) + " 分",
                changeValue);
    }

    /**
     * deadline_at 懒检查（V2.1 原子协议）：
     * 通过条件更新 CAS 保证只有一个并发调用执行副作用，rowsAffected=0 时说明另一线程已关闭，跳过。
     */
    private void lazyCloseIfDeadlinePassed(ExitVote vote) {
        if (!"voting".equals(vote.getStatus())) return;
        if (vote.getDeadlineAt() == null || !LocalDateTime.now().isAfter(vote.getDeadlineAt())) return;
        boolean pass = vote.getAgreeCount() > vote.getDisagreeCount();
        String result = pass ? "pass" : "reject";
        LocalDateTime now = LocalDateTime.now();
        // 原子条件更新：status='voting' 时才写入，防止并发重复执行副作用
        int rows = baseMapper.update(null, new UpdateWrapper<ExitVote>()
                .eq("id", vote.getId())
                .eq("status", "voting")
                .set("status", "closed")
                .set("result", result)
                .set("closed_at", now)
                .set("updated_at", now));
        if (rows == 0) {
            // 另一线程已关闭，刷新 vote 对象供调用方感知最新状态
            ExitVote latest = this.getById(vote.getId());
            if (latest != null) {
                vote.setStatus(latest.getStatus());
                vote.setResult(latest.getResult());
            }
            return;
        }
        vote.setStatus("closed");
        vote.setResult(result);
        vote.setClosedAt(now);
        if (pass) {
            executePassedExitSideEffects(vote);
        }
    }

    /**
     * T-146: 写退出扣分流水并更新 user.credit_score，校验写入成功。
     * changeValue 为负数（如 -5 或 -10）。
     */
    private void writeCreditChange(Long userId, Long projectId, String changeType,
                                   String sourceType, Long sourceId, String description, int changeValue) {
        CreditChange cc = new CreditChange();
        cc.setUserId(userId);
        cc.setProjectId(projectId);
        cc.setChangeType(changeType);
        cc.setChangeValue(changeValue);
        cc.setEffective(true);
        cc.setSourceType(sourceType);
        cc.setSourceId(sourceId);
        cc.setDescription(description);
        cc.setCreatedAt(LocalDateTime.now());
        cc.setUpdatedAt(LocalDateTime.now());
        int insertRows = creditChangeMapper.insert(cc);
        if (insertRows != 1) {
            throw new RuntimeException("写入 credit_change 失败");
        }
        int updateRows = userMapper.updateCreditScore(userId, changeValue);
        if (updateRows != 1) {
            throw new RuntimeException("更新 user.credit_score 失败");
        }
    }

    private TeamMember getActiveMember(Long projectId, Long userId) {
        return teamMemberService.getOne(new LambdaQueryWrapper<TeamMember>()
                .eq(TeamMember::getProjectId, projectId)
                .eq(TeamMember::getUserId, userId)
                .eq(TeamMember::getStatus, "active"));
    }

    // ------------------------------------------------------------------ helper: Entity → VO

    private ExitVoteVO toVO(ExitVote entity) {
        ExitVoteVO vo = new ExitVoteVO();
        vo.setId(entity.getId());
        vo.setProjectId(entity.getProjectId());
        vo.setTargetUserId(entity.getTargetUserId());
        vo.setInitiatorId(entity.getInitiatorId());
        vo.setStatus(entity.getStatus());
        vo.setPenaltyLevel(entity.getPenaltyLevel());
        vo.setResult(entity.getResult());
        vo.setReason(entity.getReason());
        vo.setTotalVoters(entity.getTotalVoters());
        vo.setAgreeCount(entity.getAgreeCount());
        vo.setDisagreeCount(entity.getDisagreeCount());
        vo.setDeadlineAt(entity.getDeadlineAt());
        vo.setClosedAt(entity.getClosedAt());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
