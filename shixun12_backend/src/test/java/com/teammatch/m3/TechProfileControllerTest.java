package com.teammatch.m3;

import com.teammatch.common.ReasonCode;
import com.teammatch.common.Result;
import com.teammatch.controller.TechProfileController;
import com.teammatch.dto.LeaderboardEntryVO;
import com.teammatch.dto.TechProfileVO;
import com.teammatch.exception.AuthenticationException;
import com.teammatch.service.TechProfileService;
import com.teammatch.util.AuthUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * M3 技术画像控制器单元测试
 */
@ExtendWith(MockitoExtension.class)
class TechProfileControllerTest {

    @Mock
    private TechProfileService techProfileService;

    @Mock
    private AuthUtil authUtil;

    @InjectMocks
    private TechProfileController controller;

    private static final String TOKEN = "Bearer test-token";

    @BeforeEach
    void setUp() {
        lenient().when(authUtil.requireUserId(TOKEN)).thenReturn(2L);
    }

    @Test
    @DisplayName("获取当前用户技术画像 - 成功")
    void getMyTechProfile_shouldReturnVO() {
        TechProfileVO vo = new TechProfileVO();
        vo.setId(1L);
        vo.setGithubUsername("test");
        vo.setTechScore(1000);
        vo.setClaimed(true);

        when(techProfileService.getProfileByUserId(2L)).thenReturn(vo);

        Result<TechProfileVO> result = controller.getMyTechProfile(TOKEN);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getGithubUsername()).isEqualTo("test");
    }

    @Test
    @DisplayName("获取当前用户技术画像 - 未认领返回 M3026")
    void getMyTechProfile_shouldReturnErrorWhenNotFound() {
        when(techProfileService.getProfileByUserId(2L)).thenReturn(null);

        Result<TechProfileVO> result = controller.getMyTechProfile(TOKEN);

        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.TECH_PROFILE_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("获取排行榜 - 分页成功")
    void getLeaderboard_shouldReturnPaginatedList() {
        LeaderboardEntryVO entry1 = new LeaderboardEntryVO();
        entry1.setRank(1);
        entry1.setGithubUsername("user1");
        entry1.setTechScore(5000);

        LeaderboardEntryVO entry2 = new LeaderboardEntryVO();
        entry2.setRank(2);
        entry2.setGithubUsername("user2");
        entry2.setTechScore(3000);

        when(techProfileService.getLeaderboard(1, 10)).thenReturn(Arrays.asList(entry1, entry2));

        Result<List<LeaderboardEntryVO>> result = controller.getLeaderboard(1, 10);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(2);
        assertThat(result.getData().get(0).getRank()).isEqualTo(1);
    }

    @Test
    @DisplayName("获取排行榜总数 - 成功")
    void getLeaderboardCount_shouldReturnCount() {
        when(techProfileService.getLeaderboardCount()).thenReturn(5L);

        Result<Long> result = controller.getLeaderboardCount();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo(5L);
    }

    @Test
    @DisplayName("获取指定用户技术画像 - 成功")
    void getUserTechProfile_shouldReturnVO() {
        TechProfileVO vo = new TechProfileVO();
        vo.setId(1L);
        vo.setGithubUsername("other");
        vo.setTechScore(2000);

        when(techProfileService.getProfileByUserId(3L)).thenReturn(vo);

        Result<TechProfileVO> result = controller.getUserTechProfile(3L);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getGithubUsername()).isEqualTo("other");
    }
}