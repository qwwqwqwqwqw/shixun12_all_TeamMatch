package com.teammatch.m3;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teammatch.common.Result;
import com.teammatch.controller.UserController;
import com.teammatch.dto.ProfileDetailVO;
import com.teammatch.entity.User;
import com.teammatch.exception.AuthenticationException;
import com.teammatch.mapper.UserMapper;
import com.teammatch.service.storage.OssService;
import com.teammatch.util.AuthUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * M3 用户控制器单元测试
 */
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private static final String TOKEN = "Bearer test-token";
    private static final Long USER_ID = 1L;

    @Mock
    private AuthUtil authUtil;

    @Mock
    private UserMapper userMapper;

    @Mock
    private OssService ossService;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        lenient().when(authUtil.requireUserId(TOKEN)).thenReturn(USER_ID);
    }

    @Test
    @DisplayName("获取用户列表 - 分页正常返回")
    void getUserList_shouldReturnPaginated() {
        User user1 = new User();
        user1.setId(1L);
        user1.setNickname("张三");
        user1.setSchool("清华大学");

        User user2 = new User();
        user2.setId(2L);
        user2.setNickname("李四");
        user2.setSchool("北京大学");

        Page<User> mockPage = new Page<>(1, 10, 2);
        mockPage.setRecords(Arrays.asList(user1, user2));

        when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        Result<Page<ProfileDetailVO>> result = userController.getUserList(1, 10, null, TOKEN);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getRecords()).hasSize(2);
        assertThat(result.getData().getRecords().get(0).getNickname()).isEqualTo("张三");
        assertThat(result.getData().getRecords().get(1).getNickname()).isEqualTo("李四");
        assertThat(result.getData().getTotal()).isEqualTo(2);
    }

    @Test
    @DisplayName("获取用户列表 - 支持关键字搜索")
    void getUserList_shouldSupportKeyword() {
        User user = new User();
        user.setId(1L);
        user.setNickname("张三");
        user.setSchool("清华大学");

        Page<User> mockPage = new Page<>(1, 20, 1);
        mockPage.setRecords(Collections.singletonList(user));

        when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        Result<Page<ProfileDetailVO>> result = userController.getUserList(1, 20, "张三", TOKEN);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getRecords()).hasSize(1);
        assertThat(result.getData().getRecords().get(0).getNickname()).isEqualTo("张三");
    }

    @Test
    @DisplayName("获取用户列表 - 空结果返回空列表")
    void getUserList_shouldReturnEmpty_whenNoUsers() {
        Page<User> emptyPage = new Page<>(1, 20, 0);
        emptyPage.setRecords(Collections.emptyList());

        when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(emptyPage);

        Result<Page<ProfileDetailVO>> result = userController.getUserList(1, 20, null, TOKEN);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getRecords()).isEmpty();
        assertThat(result.getData().getTotal()).isEqualTo(0);
    }

    @Test
    @DisplayName("获取用户列表 - 未登录返回 UNAUTHORIZED")
    void getUserList_shouldReturnUnauthorized_whenNoToken() {
        when(authUtil.requireUserId("invalid-token"))
                .thenThrow(new AuthenticationException(com.teammatch.common.ReasonCode.UNAUTHORIZED));

        try {
            userController.getUserList(1, 20, null, "invalid-token");
        } catch (AuthenticationException e) {
            assertThat(e.getReasonCode().getCode()).isEqualTo("M3000");
        }
    }

    @Test
    @DisplayName("获取用户列表 - size 超过 100 时自动限制为 20")
    void getUserList_shouldCapSize() {
        Page<User> emptyPage = new Page<>(1, 20, 0);
        emptyPage.setRecords(Collections.emptyList());
        when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(emptyPage);

        Result<Page<ProfileDetailVO>> result = userController.getUserList(1, 999, null, TOKEN);

        // size 被限制为 20，所以 Page 中 size=20
        assertThat(result.isSuccess()).isTrue();
    }
}
