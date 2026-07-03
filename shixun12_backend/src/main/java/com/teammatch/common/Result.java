package com.teammatch.common;

import java.io.Serializable;

/**
 * 统一响应结果类
 * 用于封装所有 API 的返回结果
 *
 * @param <T> 业务数据类型
 */
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 业务状态码
     * 对应 ReasonCode.code
     */
    private String code;

    /**
     * 业务状态描述
     * 对应 ReasonCode.message
     */
    private String message;

    /**
     * 业务数据
     * 成功时携带具体数据，失败时可为 null
     */
    private T data;

    // ==================== 构造方法 ====================

    /**
     * 私有构造方法，强制使用静态工厂方法创建
     */
    private Result() {
    }

    /**
     * 私有构造方法
     */
    private Result(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 创建成功响应（无数据）
     */
    public static <T> Result<T> success() {
        return new Result<>(
            ReasonCode.SUCCESS.getCode(),
            ReasonCode.SUCCESS.getMessage(),
            null
        );
    }

    /**
     * 创建成功响应（携带数据）
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(
            ReasonCode.SUCCESS.getCode(),
            ReasonCode.SUCCESS.getMessage(),
            data
        );
    }

    /**
     * 创建失败响应（使用 ReasonCode）
     */
    public static <T> Result<T> fail(ReasonCode reasonCode) {
        return new Result<>(
            reasonCode.getCode(),
            reasonCode.getMessage(),
            null
        );
    }

    /**
     * 创建失败响应（使用 ReasonCode + 自定义数据）
     */
    public static <T> Result<T> fail(ReasonCode reasonCode, T data) {
        return new Result<>(
            reasonCode.getCode(),
            reasonCode.getMessage(),
            data
        );
    }

    /**
     * 创建自定义响应
     */
    public static <T> Result<T> of(String code, String message, T data) {
        return new Result<>(code, message, data);
    }

    // ==================== 判断方法 ====================

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return ReasonCode.SUCCESS.getCode().equals(this.code);
    }

    /**
     * 判断是否失败
     */
    public boolean isFail() {
        return !isSuccess();
    }

    // ==================== Getter/Setter ====================

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Result{" +
                "code='" + code + '\'' +
                ", message='" + message + '\'' +
                ", data=" + data +
                '}';
    }
}
