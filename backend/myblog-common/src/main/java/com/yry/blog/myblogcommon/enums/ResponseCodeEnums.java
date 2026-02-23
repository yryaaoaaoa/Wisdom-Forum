package com.yry.blog.myblogcommon.enums;

import lombok.Getter;

/**
 * 统一响应状态码枚举
 */
@Getter
public enum ResponseCodeEnums {
    // 成功状态码
    SUCCESS(200, "操作成功"),

    // 客户端错误（4xx）
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权访问"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),

    // 业务相关错误
    USERNAME_OR_PASSWORD_ERROR(4000, "用户名或密码错误"),
    USER_EXIST(4001, "用户已存在"),
    USER_NOT_EXIST(4002, "用户不存在"),
    USER_DISABLED(4003, "账号已禁用"),
    ROLE_NOT_EXIST(4004, "角色不存在"),
    PERMISSION_DENIED(4005, "权限不足"),
    DATA_EXISTS(4006, "数据已存在"),
    DATA_NOT_EXISTS(4007, "数据不存在"),
    PASSWORD_EMPTY(4008, "密码不能为空"),
    OLD_PASSWORD_ERROR(4009, "旧密码错误"),
    NEW_PASSWORD_SAME_AS_OLD(4010, "新密码不能与旧密码相同"),
    PASSWORD_COMPLEXITY_FAILED(4011, "密码复杂度不足"),
    PASSWORD_RESET_FAILED(4012, "密码重置失败"),
    USELESS_TOKEN(4013, "无效的Token"),
    CAPTCHA_ERROR(4014, "验证码错误"),
    EMAIL_ERROR(4015, "邮箱错误"),

    // 服务器错误（5xx）
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),

    // 文件/上传相关
    FILE_UPLOAD_FAILED(6001, "文件上传失败"),
    FILE_TYPE_NOT_ALLOWED(6002, "文件类型不允许"),
    FILE_SIZE_EXCEEDED(6003, "文件大小超出限制"),
    UPLOAD_TYPE_INVALID(6004, "上传文件类型无效"), // 对应：前端传的fileType不在枚举中
    ARTICLE_IMAGE_MISSING_ARTICLE_ID(6005, "文章插图上传必须传入文章ID"), // 核心：插图未传articleId
    ARTICLE_ID_INVALID(6006, "文章ID无效（必须为正整数）"); // 补充：articleId格式错误

    private final int code;
    private final String message;

    ResponseCodeEnums(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 根据code获取枚举实例
     */
    public static ResponseCodeEnums getByCode(int code) {
        for (ResponseCodeEnums value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return INTERNAL_SERVER_ERROR; // 默认返回服务器错误
    }
}