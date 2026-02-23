package com.yry.blog.myblogcommon.captcha.dto;

import lombok.Data;

/**
 * 验证码响应数据传输对象
 */
@Data
public class CaptchaResponse {
    /**
     * 验证码唯一标识key
     */
    private String key;

    /**
     * 验证码图片的Base64编码
     */
    private String imageBase64;

}
