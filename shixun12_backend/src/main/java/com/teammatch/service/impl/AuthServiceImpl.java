package com.teammatch.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teammatch.common.ReasonCode;
import com.teammatch.config.WeChatProperties;
import com.teammatch.dto.LoginRequest;
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
import com.teammatch.mapper.UserMapper;
import com.teammatch.service.AuthService;
import com.teammatch.service.EmailService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * M3 认证服务实现类
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private EmailService emailService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private WeChatProperties weChatProperties;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String CODE_PREFIX = "email:code:";
    private static final String SEND_LIMIT_PREFIX = "email:send:";
    private static final String TOKEN_PREFIX = "auth:token:";
    private static final String JWT_SECRET = "TeamMatchSecretKey2024SecureJwtTokenForHS256Algorithm";
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();
    // 使用 SecureRandom 生成更安全的验证码
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    public User wechatLogin(LoginRequest request) {
        // 1. 调微信 code2session 接口，用 code 换取 openid 和 session_key
        String url = String.format(
                "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                weChatProperties.getAppId(),
                weChatProperties.getSecret(),
                request.getCode()
        );

        // 微信接口返回 Content-Type: text/plain，先用 String 接收再手动解析
        String responseBody;
        try {
            responseBody = restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            log.error("微信 code2session 接口调用失败: {}", e.getMessage());
            throw new AuthenticationException(ReasonCode.WECHAT_LOGIN_FAILED, "微信服务器请求失败");
        }

        if (responseBody == null || responseBody.isEmpty()) {
            throw new AuthenticationException(ReasonCode.WECHAT_LOGIN_FAILED, "微信服务器无响应");
        }

        Map<String, Object> wxResponse;
        try {
            wxResponse = objectMapper.readValue(responseBody,
                    new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("微信 code2session 响应解析失败: {}", responseBody);
            throw new AuthenticationException(ReasonCode.WECHAT_LOGIN_FAILED, "微信服务器返回格式异常");
        }

        // 2. 检查微信返回的错误码
        Integer errcode = (Integer) wxResponse.get("errcode");
        String errmsg = (String) wxResponse.get("errmsg");
        if (errcode != null && errcode != 0) {
            log.warn("微信登录失败: errcode={}, errmsg={}", errcode, errmsg);
            // 常见错误：40029 code 无效/过期，45011 频率限制，40226 高风险
            throw new AuthenticationException(ReasonCode.WECHAT_LOGIN_FAILED,
                    "微信登录失败: " + (errmsg != null ? errmsg : "未知错误"));
        }

        // 3. 获取 openid（session_key 不需要持久化，直接丢弃）
        String openid = (String) wxResponse.get("openid");
        if (openid == null || openid.isEmpty()) {
            throw new AuthenticationException(ReasonCode.WECHAT_LOGIN_FAILED, "未获取到微信身份标识");
        }

        // 4. 用 openid 查找或创建用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getOpenid, openid);
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            // 默认昵称：取 openid 前 8 位，用户可在首次登录时修改
            user.setNickname("User_" + openid.substring(0, Math.min(8, openid.length())));
            user.setCreditScore(100);
            user.setRole("user");
            user.setStatus("active");
            userMapper.insert(user);
            log.info("新用户通过微信登录创建: openid={}", openid);
        } else if (!"active".equals(user.getStatus())) {
            throw new AuthenticationException(ReasonCode.ACCOUNT_BANNED, "账号已被封禁");
        }

        return user;
    }

    @Override
    public User mockLogin(LoginRequest request) {
        log.warn("使用 Mock 登录（仅限开发测试）: code={}", request.getCode());
        // Mock 逻辑：直接使用 code 作为 openid 查找或创建用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getOpenid, request.getCode());
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            user = new User();
            user.setOpenid(request.getCode());
            user.setNickname("User_" + request.getCode().substring(0, Math.min(6, request.getCode().length())));
            user.setCreditScore(100);
            user.setRole("user");
            user.setStatus("active");
            userMapper.insert(user);
        } else if (!"active".equals(user.getStatus())) {
            throw new AuthenticationException(ReasonCode.ACCOUNT_BANNED, "账号已被封禁");
        }
        return user;
    }

    @Override
    public User passwordLogin(PasswordLoginRequest request) {
        // 参数校验
        if (request.getUsername() == null || request.getUsername().isEmpty()) {
            throw new ValidationException(ReasonCode.USERNAME_REQUIRED);
        }
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new ValidationException(ReasonCode.PASSWORD_REQUIRED, "密码不能为空");
        }

        // 根据用户名查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new AuthenticationException(ReasonCode.INVALID_PASSWORD);
        }

        // 检查用户是否设置了密码（在验证密码之前检查，避免 M3014 被 M3013 拦截）
        if (user.getPasswordHash() == null || user.getPasswordHash().isEmpty()) {
            throw new AuthenticationException(ReasonCode.PASSWORD_NOT_SET);
        }

        // 验证密码
        if (!PASSWORD_ENCODER.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AuthenticationException(ReasonCode.INVALID_PASSWORD);
        }

        // 检查用户状态
        if (!"active".equals(user.getStatus())) {
            throw new AuthenticationException(ReasonCode.ACCOUNT_BANNED, "账号已被封禁");
        }

        // V2.1 规范：密码登录仅适用于管理员，普通用户使用微信登录
        if (user.getRole() == null || !"admin".equals(user.getRole())) {
            throw new AuthorizationException(ReasonCode.ADMIN_REQUIRED);
        }

        return user;
    }

    @Override
    public void sendEmailCode(SendEmailCodeRequest request, String token) {
        // 1. 校验邮箱格式
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new ValidationException(ReasonCode.INVALID_EMAIL, "邮箱不能为空");
        }
        // 简单邮箱格式校验
        if (!request.getEmail().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new ValidationException(ReasonCode.INVALID_EMAIL, "邮箱格式不正确");
        }
        
        Long userId = getUserIdFromToken(token);
        
        // 2. 检查该邮箱是否已被其他用户认证绑定
        LambdaQueryWrapper<User> emailCheck = new LambdaQueryWrapper<>();
        emailCheck.eq(User::getEmail, request.getEmail())
                .eq(User::getEmailVerified, true);
        User emailOwner = userMapper.selectOne(emailCheck);
        if (emailOwner != null && !emailOwner.getId().equals(userId)) {
            throw new DuplicateDataException(ReasonCode.EMAIL_OCCUPIED);
        }

        // 3. 60秒防刷检查
        String sendLimitKey = SEND_LIMIT_PREFIX + userId;
        Boolean exists = redisTemplate.hasKey(sendLimitKey);
        if (Boolean.TRUE.equals(exists)) {
            throw new ValidationException(ReasonCode.SEND_CODE_TOO_FREQUENT);
        }
        
        // 3. 生成验证码（使用 SecureRandom）
        String code = String.format("%06d", SECURE_RANDOM.nextInt(1000000));
        String codeKey = CODE_PREFIX + userId;
        
        // 4. 先发送邮件，成功后再设置 Redis
        try {
            emailService.sendVerificationCode(request.getEmail(), code);
            
            // 邮件发送成功后，设置验证码和防刷标记
            redisTemplate.opsForValue().set(codeKey, code, 10, TimeUnit.MINUTES);
            redisTemplate.opsForValue().set(sendLimitKey, "1", 60, TimeUnit.SECONDS);
            
            log.info("邮箱验证码发送成功: userId={}, email={}", userId, request.getEmail());
        } catch (Exception e) {
            log.error("邮箱验证码发送失败: userId={}, email={}, error={}", userId, request.getEmail(), e.getMessage());
            // 发送失败时，确保不留下脏数据
            redisTemplate.delete(codeKey);
            redisTemplate.delete(sendLimitKey);
            throw new RuntimeException(ReasonCode.EMAIL_SEND_FAILED.getMessage(), e);
        }
    }

    @Override
    public boolean verifyEmailCode(VerifyEmailCodeRequest request, String token) {
        Long userId = getUserIdFromToken(token);
        String key = CODE_PREFIX + userId;
        String storedCode = redisTemplate.opsForValue().get(key);
        
        if (storedCode != null && storedCode.equals(request.getCode())) {
            redisTemplate.delete(key);
            
            // 检查该邮箱是否已被其他用户占用或已认证
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getEmail, request.getEmail());
            User existingUser = userMapper.selectOne(wrapper);
            
            if (existingUser != null) {
                // 如果邮箱已被其他用户绑定，禁止重复绑定
                if (!existingUser.getId().equals(userId)) {
                    throw new DuplicateDataException(ReasonCode.EMAIL_OCCUPIED);
                }
                // 如果邮箱已被当前用户认证过，禁止重复绑定
                if (Boolean.TRUE.equals(existingUser.getEmailVerified())) {
                    throw new DuplicateDataException(ReasonCode.EMAIL_ALREADY_VERIFIED);
                }
            }
            
            User user = userMapper.selectById(userId);
            if (user != null) {
                user.setEmail(request.getEmail());
                user.setEmailVerified(true);
                
                // 【实习要点4】邮箱验证成功后，重新计算正式档案状态
                // 避免用户先填写档案再验证邮箱时，formalProfileCompleted 没有更新
                user.updateFormalProfileCompleted();
                
                userMapper.updateById(user);
            }
            return true;
        }
        return false;
    }

    @Override
    public void adminCreatePassword(PasswordRequest request, String token) {
        // 【实习要点1】管理员权限校验：任何管理端接口都必须验证当前用户是否为管理员
        // 面试时可以说："我实现了基于角色的访问控制（RBAC），确保只有管理员才能操作敏感数据"
        Long adminId = getUserIdFromToken(token);
        User admin = userMapper.selectById(adminId);
        if (admin == null || !"admin".equals(admin.getRole())) {
            throw new AuthorizationException(ReasonCode.ADMIN_REQUIRED);
        }
        
        if (request.getUsername() == null || request.getUsername().isEmpty()) {
            throw new ValidationException(ReasonCode.USERNAME_REQUIRED, "用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new ValidationException(ReasonCode.PASSWORD_TOO_SHORT);
        }
        
        // 检查用户是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        User user = userMapper.selectOne(wrapper);
        
        // V2.1 规范：普通用户统一微信登录，管理员才使用密码登录
        // 因此创建密码接口只为已存在的用户设置密码，不允许创建新用户
        if (user == null) {
            throw new NotFoundException(ReasonCode.USER_NOT_FOUND, "用户不存在，请先通过微信登录创建用户");
        }
        
        if (user.getPasswordHash() != null && !user.getPasswordHash().isEmpty()) {
            throw new DuplicateDataException(ReasonCode.PASSWORD_ALREADY_SET, "该用户已设置密码，请使用修改密码接口");
        }
        
        // 设置密码
        user.setPasswordHash(PASSWORD_ENCODER.encode(request.getPassword()));
        userMapper.updateById(user);
    }

    @Override
    public void adminUpdatePassword(PasswordRequest request, String token) {
        // 【实习要点1】管理员权限校验
        Long adminId = getUserIdFromToken(token);
        User admin = userMapper.selectById(adminId);
        if (admin == null || !"admin".equals(admin.getRole())) {
            throw new AuthorizationException(ReasonCode.ADMIN_REQUIRED);
        }
        
        if (request.getUsername() == null || request.getUsername().isEmpty()) {
            throw new ValidationException(ReasonCode.USERNAME_REQUIRED, "用户名不能为空");
        }
        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            throw new ValidationException(ReasonCode.PASSWORD_TOO_SHORT);
        }
        
        // 查找用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        User user = userMapper.selectOne(wrapper);
        
        if (user == null) {
            throw new NotFoundException(ReasonCode.USER_NOT_FOUND);
        }
        
        // 如果提供了旧密码，需要验证旧密码是否正确
        if (request.getOldPassword() != null && !request.getOldPassword().isEmpty()) {
            if (user.getPasswordHash() == null || !PASSWORD_ENCODER.matches(request.getOldPassword(), user.getPasswordHash())) {
                throw new AuthenticationException(ReasonCode.OLD_PASSWORD_INCORRECT);
            }
        }
        
        // 更新密码
        user.setPasswordHash(PASSWORD_ENCODER.encode(request.getNewPassword()));
        userMapper.updateById(user);
    }

    @Override
    public void changePassword(PasswordRequest request, String token) {
        // 参数校验
        if (request.getOldPassword() == null || request.getOldPassword().isEmpty()) {
            throw new ValidationException(ReasonCode.OLD_PASSWORD_REQUIRED, "旧密码不能为空");
        }
        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            throw new ValidationException(ReasonCode.PASSWORD_TOO_SHORT);
        }
        
        // 获取当前用户
        Long userId = getUserIdFromToken(token);
        User user = userMapper.selectById(userId);
        
        if (user == null) {
            throw new NotFoundException(ReasonCode.USER_NOT_FOUND);
        }
        
        // 检查用户是否设置了密码
        if (user.getPasswordHash() == null || user.getPasswordHash().isEmpty()) {
            throw new AuthenticationException(ReasonCode.PASSWORD_NOT_SET, "该用户未设置密码，请联系管理员");
        }
        
        // 验证旧密码
        if (!PASSWORD_ENCODER.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new AuthenticationException(ReasonCode.OLD_PASSWORD_INCORRECT);
        }
        
        // 检查新旧密码是否相同
        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new ValidationException(ReasonCode.SAME_OLD_NEW_PASSWORD, "新密码不能与旧密码相同");
        }
        
        // 更新密码
        user.setPasswordHash(PASSWORD_ENCODER.encode(request.getNewPassword()));
        userMapper.updateById(user);
    }

    @Override
    public void bindUsername(UsernameRequest request, String token) {
        if (request.getUsername() == null || request.getUsername().isEmpty()) {
            throw new ValidationException(ReasonCode.USERNAME_REQUIRED, "用户名不能为空");
        }
        
        Long userId = getUserIdFromToken(token);
        User user = userMapper.selectById(userId);
        
        if (user == null) {
            throw new NotFoundException(ReasonCode.USER_NOT_FOUND);
        }
        
        // 如果已经绑定过 username，不允许重复绑定
        if (user.getUsername() != null && !user.getUsername().isEmpty()) {
            throw new DuplicateDataException(ReasonCode.USERNAME_ALREADY_BOUND);
        }
        
        // 检查用户名是否已被其他用户占用
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        User existingUser = userMapper.selectOne(wrapper);
        
        if (existingUser != null && !existingUser.getId().equals(userId)) {
            throw new DuplicateDataException(ReasonCode.USERNAME_OCCUPIED);
        }
        
        user.setUsername(request.getUsername());
        userMapper.updateById(user);
    }

    @Override
    public void updateUsername(UsernameRequest request, String token) {
        if (request.getUsername() == null || request.getUsername().isEmpty()) {
            throw new ValidationException(ReasonCode.USERNAME_REQUIRED, "用户名不能为空");
        }
        
        Long userId = getUserIdFromToken(token);
        User user = userMapper.selectById(userId);
        
        if (user == null) {
            throw new NotFoundException(ReasonCode.USER_NOT_FOUND);
        }
        
        // 检查用户名是否已被其他用户占用
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        User existingUser = userMapper.selectOne(wrapper);
        
        if (existingUser != null && !existingUser.getId().equals(userId)) {
            throw new DuplicateDataException(ReasonCode.USERNAME_OCCUPIED, "该用户名已被占用");
        }
        
        user.setUsername(request.getUsername());
        userMapper.updateById(user);
    }

    @Override
    public boolean isAdmin(String token) {
        // 从 Token 中解析用户ID
        Long userId = getUserIdFromToken(token);
        
        // 查询用户信息
        User user = userMapper.selectById(userId);
        
        // 检查用户是否存在且角色为 admin
        return user != null && "admin".equals(user.getRole());
    }

    @Override
    public Long getUserIdFromToken(String token) {
        // 移除 "Bearer " 前缀
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        // 直接从 JWT 中解析 Subject (userId)
        try {
            String userIdStr = Jwts.parser()
                    .setSigningKey(JWT_SECRET)
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
            return Long.parseLong(userIdStr);
        } catch (Exception e) {
            throw new AuthenticationException(ReasonCode.UNAUTHORIZED);
        }
    }

    /**
     * 生成 JWT Token（供 Controller 调用）
     */
    public String generateToken(User user) {
        String token = Jwts.builder()
                .setSubject(String.valueOf(user.getId()))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000)) // 7天
                .signWith(SignatureAlgorithm.HS256, JWT_SECRET)
                .compact();
        
        // 将 token 与 userId 关联存储到 Redis
        redisTemplate.opsForValue().set(TOKEN_PREFIX + token, String.valueOf(user.getId()), 7, TimeUnit.DAYS);
        
        return token;
    }
}
