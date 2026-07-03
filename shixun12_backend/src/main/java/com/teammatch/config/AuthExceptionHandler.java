package com.teammatch.config;

import com.teammatch.common.ReasonCode;
import com.teammatch.common.Result;
import com.teammatch.exception.AuthenticationException;
import com.teammatch.exception.AuthorizationException;
import com.teammatch.exception.DuplicateDataException;
import com.teammatch.exception.NotFoundException;
import com.teammatch.exception.ValidationException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 认证相关全局异常处理器
 * 
 * 处理的异常类型：
 * - MalformedJwtException: Token 格式错误
 * - ExpiredJwtException: Token 已过期
 * - SignatureException: Token 签名验证失败
 * - AuthenticationException: 认证失败（M3000）
 * - AuthorizationException: 权限不足（M3009）
 */
@Slf4j
@RestControllerAdvice
public class AuthExceptionHandler {

    /**
     * 处理 JWT 相关异常
     * 
     * @param e JWT 异常
     * @return M3000 UNAUTHORIZED
     */
    @ExceptionHandler({MalformedJwtException.class, ExpiredJwtException.class, SignatureException.class})
    public Result<Void> handleJwtException(Exception e) {
        log.warn("JWT Token 解析失败: {}", e.getMessage());
        return Result.fail(ReasonCode.UNAUTHORIZED);
    }

    /**
     * 处理认证失败异常（M3000）
     * 
     * @param e 认证异常
     * @return M3000 UNAUTHORIZED
     */
    @ExceptionHandler(AuthenticationException.class)
    public Result<Void> handleAuthenticationException(AuthenticationException e) {
        log.warn("认证失败: {}", e.getReasonCode());
        return Result.fail(e.getReasonCode());
    }

    /**
     * 处理权限不足异常（M3009）
     * 
     * @param e 权限异常
     * @return M3009 ADMIN_REQUIRED
     */
    @ExceptionHandler(AuthorizationException.class)
    public Result<Void> handleAuthorizationException(AuthorizationException e) {
        log.warn("权限不足: {}", e.getReasonCode());
        return Result.fail(e.getReasonCode());
    }

    /**
     * 处理数据重复异常
     * 
     * @param e 数据重复异常
     * @return 对应的错误码
     */
    @ExceptionHandler(DuplicateDataException.class)
    public Result<Void> handleDuplicateDataException(DuplicateDataException e) {
        log.warn("数据重复: {}", e.getReasonCode());
        return Result.fail(e.getReasonCode());
    }

    @ExceptionHandler(NotFoundException.class)
    public Result<Void> handleNotFoundException(NotFoundException e) {
        log.warn("资源不存在: {}", e.getReasonCode());
        return Result.fail(e.getReasonCode());
    }

    @ExceptionHandler(ValidationException.class)
    public Result<Void> handleValidationException(ValidationException e) {
        log.warn("参数校验失败: {}", e.getReasonCode());
        return Result.fail(e.getReasonCode());
    }
}
