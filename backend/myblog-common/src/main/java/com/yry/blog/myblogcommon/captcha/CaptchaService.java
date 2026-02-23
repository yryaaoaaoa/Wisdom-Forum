package com.yry.blog.myblogcommon.captcha;

import com.yry.blog.myblogcommon.captcha.dto.CaptchaResponse;

public interface CaptchaService {
    CaptchaResponse generateCaptcha();
    boolean validateCaptcha(String key, String input);
}