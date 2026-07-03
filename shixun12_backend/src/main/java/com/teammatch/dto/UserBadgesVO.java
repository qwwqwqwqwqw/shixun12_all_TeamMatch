package com.teammatch.dto;

/**
 * 用户角标聚合 VO
 * 用于 GET /api/me/badges 接口，汇总当前用户待处理的事项数量
 */
public class UserBadgesVO {

    /** 待处理的组队请求（邀请/申请）数量 */
    private int pendingInvites;

    /** 待处理的退出投票数量 */
    private int pendingVotes;

    /** 待完成的互评数量 */
    private int pendingEvaluations;

    /** 总待处理数量 */
    private int total;

    public UserBadgesVO() {
    }

    public UserBadgesVO(int pendingInvites, int pendingVotes, int pendingEvaluations) {
        this.pendingInvites = pendingInvites;
        this.pendingVotes = pendingVotes;
        this.pendingEvaluations = pendingEvaluations;
        this.total = pendingInvites + pendingVotes + pendingEvaluations;
    }

    public int getPendingInvites() { return pendingInvites; }
    public void setPendingInvites(int pendingInvites) { this.pendingInvites = pendingInvites; }

    public int getPendingVotes() { return pendingVotes; }
    public void setPendingVotes(int pendingVotes) { this.pendingVotes = pendingVotes; }

    public int getPendingEvaluations() { return pendingEvaluations; }
    public void setPendingEvaluations(int pendingEvaluations) { this.pendingEvaluations = pendingEvaluations; }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
}
