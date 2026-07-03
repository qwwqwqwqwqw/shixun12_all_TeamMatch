package com.teammatch.m3;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teammatch.dto.LeaderboardEntryVO;
import com.teammatch.dto.TechProfileVO;
import com.teammatch.entity.TechProfile;
import com.teammatch.entity.User;
import com.teammatch.mapper.TechProfileMapper;
import com.teammatch.mapper.UserMapper;
import com.teammatch.service.impl.TechProfileServiceImpl;
import com.teammatch.service.storage.OssService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * M3 技术画像服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class TechProfileServiceTest {

    @Mock
    private TechProfileMapper techProfileMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private org.springframework.web.client.RestTemplate restTemplate;

    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Mock
    private com.teammatch.service.impl.GiteeSyncService giteeSyncService;

    @Mock
    private com.teammatch.service.impl.GitHubSyncService gitHubSyncService;

    @Mock
    private OssService ossService;

    @InjectMocks
    private TechProfileServiceImpl techProfileService;

    private TechProfile sampleProfile;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleProfile = new TechProfile();
        sampleProfile.setId(1L);
        sampleProfile.setGithubUsername("testuser");
        sampleProfile.setClaimedByUserId(2L);
        sampleProfile.setTotalStars(100);
        sampleProfile.setTotalRepos(10);
        sampleProfile.setTotalCommits(500);
        sampleProfile.setTotalPrs(20);
        sampleProfile.setTotalContributions(300);
        sampleProfile.setTopLanguages("[\"Java\",\"Python\"]");
        sampleProfile.setTechScore(2850);
        sampleProfile.setBio("Test bio");
        sampleProfile.setAvatarUrl("https://avatars.githubusercontent.com/u/test");
        sampleProfile.setLastSyncedAt(LocalDateTime.now());

        sampleUser = new User();
        sampleUser.setId(2L);
        sampleUser.setNickname("TestUser");
        sampleUser.setNickname("测试用户");
        sampleUser.setSchool("测试大学");
        sampleUser.setTechProfileId(1L);
    }

    @Test
    @DisplayName("获取用户技术画像 - 有画像时返回 VO")
    void getProfileByUserId_shouldReturnVO() {
        when(userMapper.selectById(2L)).thenReturn(sampleUser);
        when(techProfileMapper.selectById(1L)).thenReturn(sampleProfile);

        TechProfileVO vo = techProfileService.getProfileByUserId(2L);

        assertThat(vo).isNotNull();
        assertThat(vo.getGithubUsername()).isEqualTo("testuser");
        assertThat(vo.getTechScore()).isEqualTo(2850);
        assertThat(vo.getClaimed()).isTrue();
    }

    @Test
    @DisplayName("获取用户技术画像 - 用户不存在时返回 null")
    void getProfileByUserId_shouldReturnNullWhenUserNotFound() {
        when(userMapper.selectById(999L)).thenReturn(null);

        TechProfileVO vo = techProfileService.getProfileByUserId(999L);

        assertThat(vo).isNull();
    }

    @Test
    @DisplayName("获取用户技术画像 - 未认领时返回 null")
    void getProfileByUserId_shouldReturnNullWhenNoProfile() {
        User userWithoutProfile = new User();
        userWithoutProfile.setId(5L);
        userWithoutProfile.setNickname("NoProfile");
        when(userMapper.selectById(5L)).thenReturn(userWithoutProfile);

        TechProfileVO vo = techProfileService.getProfileByUserId(5L);

        assertThat(vo).isNull();
    }

    @Test
    @DisplayName("排行榜查询 - 按 techScore 降序返回")
    void getLeaderboard_shouldReturnRankedList() {
        TechProfile tp1 = new TechProfile();
        tp1.setId(1L);
        tp1.setGithubUsername("user1");
        tp1.setClaimedByUserId(2L);
        tp1.setTechScore(5000);
        tp1.setTotalStars(200);
        tp1.setTotalRepos(15);
        tp1.setTotalCommits(800);
        tp1.setTotalPrs(30);
        tp1.setTotalContributions(500);
        tp1.setTopLanguages("[\"Java\"]");
        tp1.setBio("bio1");

        TechProfile tp2 = new TechProfile();
        tp2.setId(2L);
        tp2.setGithubUsername("user2");
        tp2.setClaimedByUserId(null);
        tp2.setTechScore(3000);
        tp2.setTotalStars(100);
        tp2.setTotalRepos(8);
        tp2.setTotalCommits(400);
        tp2.setTotalPrs(15);
        tp2.setTotalContributions(200);
        tp2.setTopLanguages("[\"Python\"]");
        tp2.setBio("bio2");

        // Mock MyBatis-Plus page query
        when(techProfileMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenAnswer(invocation -> {
                    Page<TechProfile> page = invocation.getArgument(0);
                    page.setRecords(Arrays.asList(tp1, tp2));
                    page.setTotal(2);
                    return page;
                });

        when(userMapper.selectById(2L)).thenReturn(sampleUser);

        List<LeaderboardEntryVO> result = techProfileService.getLeaderboard(1, 20);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRank()).isEqualTo(1);
        assertThat(result.get(0).getGithubUsername()).isEqualTo("user1");
        assertThat(result.get(0).getTechScore()).isEqualTo(5000);
        assertThat(result.get(0).getClaimed()).isTrue();

        assertThat(result.get(1).getRank()).isEqualTo(2);
        assertThat(result.get(1).getGithubUsername()).isEqualTo("user2");
        assertThat(result.get(1).getClaimed()).isFalse();
    }

    @Test
    @DisplayName("技术评分计算 - verify formula")
    void computeTechScore_shouldCalculateCorrectly() {
        TechProfile tp = new TechProfile();
        tp.setTotalStars(10);
        tp.setTotalCommits(20);
        tp.setTotalPrs(5);
        tp.setTotalRepos(3);
        tp.setTotalContributions(100);

        tp.computeTechScore();

        // stars*10 + commits*2 + prs*5 + repos*3 + contributions*1
        // 100 + 40 + 25 + 9 + 100 = 274
        assertThat(tp.getTechScore()).isEqualTo(274);
    }

    @Test
    @DisplayName("认领画像 - 未被认领时可认领")
    void claimProfile_shouldClaimUnclaimedProfile() {
        TechProfile unclaimed = new TechProfile();
        unclaimed.setId(1L);
        unclaimed.setGithubUsername("newuser");
        unclaimed.setClaimedByUserId(null);

        when(techProfileMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(unclaimed);
        when(userMapper.selectById(3L)).thenReturn(sampleUser);
        when(techProfileMapper.updateById(any(TechProfile.class))).thenReturn(1);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        TechProfile result = techProfileService.claimProfile("newuser", "github", 3L);

        assertThat(result.getClaimedByUserId()).isEqualTo(3L);
        verify(techProfileMapper).updateById(any(TechProfile.class));
    }
}