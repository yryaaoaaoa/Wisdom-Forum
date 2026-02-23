package com.yry.blog.myblogcommon.result;

import com.yry.blog.myblogcommon.enums.ResponseCodeEnums;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Data
public class Response<T> {
    private int code;  // 使用HTTP状态码
    private String msg;
    private T data;

    // 成功响应
    public static <T> Response<T> success(T data) {
        return new Response<>(HttpStatus.OK.value(), "success", data);
    }

    // 失败响应 - 仅使用枚举默认信息
    public static <T> Response<T> error(ResponseCodeEnums errorCode) {
        return new Response<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    // 失败响应 - 可附加自定义错误信息
    public static <T> Response<T> error(ResponseCodeEnums errorCode, String additionalMessage) {
        // 可以选择拼接枚举信息和附加信息，或者只用附加信息
        // 这里选择拼接，用冒号分隔，如果 additionalMessage 为空则不拼接
        String finalMessage = errorCode.getMessage();
        if (additionalMessage != null && !additionalMessage.isEmpty()) {
            finalMessage = errorCode.getMessage() + ": " + additionalMessage;
        }
        return new Response<>(errorCode.getCode(), finalMessage, null);
    }

    // 构造方法 + Getter/Setter (Lombok @Data 已经包含了这些)
}