package com.teammatch.m4.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.teammatch.m4.dto.TeamRequestDTO;
import com.teammatch.m4.entity.TeamRequest;
import java.util.List;

public interface TeamRequestService extends IService<TeamRequest> {
    
    /**
     * T-122/T-123 发送邀请或申请，含 T-133 拦截校验
     */
    void sendRequest(TeamRequestDTO dto, String requestType);

    /**
     * T-124 接受请求：写入 team_member，请求状态改 accepted
     */
    void acceptRequest(Long requestId, Long operatorId);

    /**
     * T-129 拒绝请求：请求状态改 rejected
     */
    void rejectRequest(Long requestId, Long operatorId);

    /**
     * T-130 取消请求：发送方取消 pending 请求，状态改 cancelled
     */
    void cancelRequest(Long requestId, Long operatorId);

    /**
     * T-131 查询用户收到或发出的请求列表
     * @param userId 用户ID
     * @param direction received/sent
     */
    List<TeamRequest> getRequestList(Long userId, String direction);
}

