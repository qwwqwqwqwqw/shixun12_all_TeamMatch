package com.teammatch.controller;

import com.teammatch.common.ReasonCode;
import com.teammatch.common.Result;
import com.teammatch.dto.LoginRequest;
import com.teammatch.dto.LoginResponse;
import com.teammatch.dto.PasswordLoginRequest;
import com.teammatch.dto.PasswordRequest;
import com.teammatch.dto.SendEmailCodeRequest;
import com.teammatch.dto.UsernameRequest;
import com.teammatch.dto.VerifyEmailCodeRequest;
import com.teammatch.entity.User;
import com.teammatch.exception.AuthenticationException;
import com.teammatch.exception.AuthorizationException;
import com.teammatch.exception.DuplicateDataException;
import com.teammatch.exception.NotFoundException;
import com.teammatch.exception.ValidationException;
import com.teammatch.service.AuthService;
import com.teammatch.service.impl.AuthServiceImpl;
import com.teammatch.service.storage.OssService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器 - M3 模块
 */
@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private Environment environment;

    @Autowired
    private OssService ossService;

    /**
     * 微信登录
     * 前端调用 wx.login() 获取临时 code，后端通过 code2session 换取 openid
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        User user = authService.wechatLogin(request);
        String token = authService.generateToken(user);
        
        // 【实习要点2】构建 DTO，只返回前端需要的字段
        LoginResponse response = new LoginResponse();
        response.setId(user.getId());
        response.setNickname(user.getNickname());
        response.setAvatarUrl(ossService.resolveAvatarUrl(user.getAvatarUrl()));
        response.setEmailVerified(user.getEmailVerified());
        response.setFormalProfileCompleted(user.getFormalProfileCompleted());
        response.setCreditScore(user.getCreditScore());
        response.setToken(token);
        
        return Result.success(response);
    }

    /**
     * Mock 登录（仅开发测试用）
     * 不调微信接口，直接把 code 当作 openid 使用
     */
    @PostMapping("/login/mock")
    public Result<LoginResponse> mockLogin(@RequestBody LoginRequest request) {
        // 环境隔离：非开发环境禁止使用 Mock 登录，防止伪造 openid
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isDev = false;
        for (String p : activeProfiles) {
            if ("dev".equals(p) || "local".equals(p)) {
                isDev = true;
                break;
            }
        }
        if (!isDev) {
            return Result.fail(ReasonCode.UNAUTHORIZED);
        }

        User user = authService.mockLogin(request);
        String token = authService.generateToken(user);

        LoginResponse response = new LoginResponse();
        response.setId(user.getId());
        response.setNickname(user.getNickname());
        response.setAvatarUrl(ossService.resolveAvatarUrl(user.getAvatarUrl()));
        response.setEmailVerified(user.getEmailVerified());
        response.setFormalProfileCompleted(user.getFormalProfileCompleted());
        response.setCreditScore(user.getCreditScore());
        response.setToken(token);

        return Result.success(response);
    }

    /**
     * 管理员密码登录
     * V2.1 规范：普通用户统一微信登录，管理员才使用 username + password
     */
    @PostMapping("/login/password")
    public Result<LoginResponse> passwordLogin(@RequestBody PasswordLoginRequest request) {
        try {
            User user = authService.passwordLogin(request);
            
            // V2.1 规范：密码登录仅适用于管理员，普通用户使用微信登录
            // Service 层已校验 role == admin，此处仅做防御性检查
            if (user.getRole() == null || !"admin".equals(user.getRole())) {
                return Result.fail(com.teammatch.common.ReasonCode.ADMIN_REQUIRED);
            }
            
            String token = authService.generateToken(user);
            
            LoginResponse response = new LoginResponse();
            response.setId(user.getId());
            response.setNickname(user.getNickname());
            response.setAvatarUrl(ossService.resolveAvatarUrl(user.getAvatarUrl()));
            response.setEmailVerified(user.getEmailVerified());
            response.setFormalProfileCompleted(user.getFormalProfileCompleted());
            response.setCreditScore(user.getCreditScore());
            response.setToken(token);
            
            return Result.success(response);
        } catch (AuthenticationException e) {
            // 认证相关异常（密码错误、未设置密码等）
            return Result.fail(e.getReasonCode());
        } catch (AuthorizationException e) {
            // 权限相关异常（非管理员）
            return Result.fail(e.getReasonCode());
        } catch (ValidationException e) {
            // 参数校验异常
            return Result.fail(e.getReasonCode());
        } catch (NotFoundException e) {
            // 资源未找到
            return Result.fail(e.getReasonCode());
        } catch (Exception e) {
            log.error("passwordLogin 未知异常, username={}", request.getUsername(), e);
            return Result.fail(ReasonCode.UNKNOWN_ERROR);
        }
    }

    /**
     * 发送邮箱验证码
     */
    @PostMapping("/email/send")
    public Result<Void> sendEmailCode(@RequestBody SendEmailCodeRequest request, 
                                @RequestHeader("Authorization") String token) {
        try {
            authService.sendEmailCode(request, token);
            return Result.success(null);
        } catch (ValidationException e) {
            // 参数校验异常（邮箱格式错误、发送过于频繁等）
            return Result.fail(e.getReasonCode());
        } catch (DuplicateDataException e) {
            // 邮箱已被其他用户认证绑定
            return Result.fail(e.getReasonCode());
        } catch (RuntimeException e) {
            // 邮件发送失败
            if (e.getMessage() != null && e.getMessage().contains(ReasonCode.EMAIL_SEND_FAILED.getMessage())) {
                return Result.fail(ReasonCode.EMAIL_SEND_FAILED);
            }
            // 其他未知异常
            return Result.fail(ReasonCode.UNKNOWN_ERROR);
        }
    }

    /**
     * 验证邮箱验证码
     */
    @PostMapping("/email/verify")
    public Result<String> verifyEmailCode(@RequestBody VerifyEmailCodeRequest request, 
                                  @RequestHeader("Authorization") String token) {
        try {
            boolean success = authService.verifyEmailCode(request, token);
            if (success) {
                return Result.success("邮箱验证成功");
            } else {
                return Result.fail(com.teammatch.common.ReasonCode.INVALID_CODE);
            }
        } catch (DuplicateDataException e) {
            // 数据重复异常（邮箱已被占用）
            return Result.fail(e.getReasonCode());
        } catch (Exception e) {
            // 未知异常
            return Result.fail(ReasonCode.UNKNOWN_ERROR);
        }
    }

    /**
     * 管理员创建用户密码
     * 需要管理员权限（role=admin）
     */
    @PostMapping("/password/create")
    public Result<Void> createPassword(@RequestBody PasswordRequest request,
                                       @RequestHeader("Authorization") String token) {
        try {
            authService.adminCreatePassword(request, token);
            return Result.success(null);
        } catch (AuthorizationException e) {
            // 权限异常
            return Result.fail(e.getReasonCode());
        } catch (ValidationException e) {
            // 参数校验异常
            return Result.fail(e.getReasonCode());
        } catch (NotFoundException e) {
            // 资源未找到
            return Result.fail(e.getReasonCode());
        } catch (DuplicateDataException e) {
            // 数据重复异常
            return Result.fail(e.getReasonCode());
        } catch (Exception e) {
            // 未知异常
            return Result.fail(ReasonCode.UNKNOWN_ERROR);
        }
    }

    /**
     * 管理员修改用户密码
     * 需要管理员权限（role=admin）
     */
    @PostMapping("/password/update")
    public Result<Void> updatePassword(@RequestBody PasswordRequest request,
                                       @RequestHeader("Authorization") String token) {
        try {
            authService.adminUpdatePassword(request, token);
            return Result.success(null);
        } catch (AuthorizationException e) {
            // 权限异常
            return Result.fail(e.getReasonCode());
        } catch (ValidationException e) {
            // 参数校验异常
            return Result.fail(e.getReasonCode());
        } catch (AuthenticationException e) {
            // 认证异常（旧密码错误）
            return Result.fail(e.getReasonCode());
        } catch (NotFoundException e) {
            // 资源未找到
            return Result.fail(e.getReasonCode());
        } catch (Exception e) {
            // 未知异常
            return Result.fail(ReasonCode.UNKNOWN_ERROR);
        }
    }

    /**
     * 用户自己修改密码
     */
    @PutMapping("/password/change")
    public Result<Void> changePassword(@RequestBody PasswordRequest request,
                                       @RequestHeader("Authorization") String token) {
        try {
            authService.changePassword(request, token);
            return Result.success(null);
        } catch (ValidationException e) {
            // 参数校验异常（密码长度、新旧密码相同等）
            return Result.fail(e.getReasonCode());
        } catch (AuthenticationException e) {
            // 认证异常（旧密码错误、未设置密码）
            return Result.fail(e.getReasonCode());
        } catch (NotFoundException e) {
            // 资源未找到
            return Result.fail(e.getReasonCode());
        } catch (Exception e) {
            // 未知异常
            return Result.fail(ReasonCode.UNKNOWN_ERROR);
        }
    }

    /**
     * 用户首次绑定用户名
     */
    @PostMapping("/username/bind")
    public Result<Void> bindUsername(@RequestBody UsernameRequest request,
                                     @RequestHeader("Authorization") String token) {
        try {
            authService.bindUsername(request, token);
            return Result.success(null);
        } catch (DuplicateDataException e) {
            // 数据重复异常（用户名已绑定、已被占用）
            return Result.fail(e.getReasonCode());
        } catch (ValidationException e) {
            // 参数校验异常
            return Result.fail(e.getReasonCode());
        } catch (NotFoundException e) {
            // 资源未找到
            return Result.fail(e.getReasonCode());
        } catch (Exception e) {
            // 未知异常
            return Result.fail(ReasonCode.UNKNOWN_ERROR);
        }
    }

    /**
     * 用户修改用户名
     */
    @PutMapping("/username/update")
    public Result<Void> updateUsername(@RequestBody UsernameRequest request,
                                       @RequestHeader("Authorization") String token) {
        try {
            authService.updateUsername(request, token);
            return Result.success(null);
        } catch (DuplicateDataException e) {
            // 数据重复异常（用户名已被占用）
            return Result.fail(e.getReasonCode());
        } catch (ValidationException e) {
            // 参数校验异常
            return Result.fail(e.getReasonCode());
        } catch (NotFoundException e) {
            // 资源未找到
            return Result.fail(e.getReasonCode());
        } catch (Exception e) {
            // 未知异常
            return Result.fail(ReasonCode.UNKNOWN_ERROR);
        }
    }

}
