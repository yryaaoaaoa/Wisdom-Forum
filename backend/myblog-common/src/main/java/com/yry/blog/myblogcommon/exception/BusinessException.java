package com.yry.blog.myblogcommon.exception;

import com.yry.blog.myblogcommon.enums.ResponseCodeEnums;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final ResponseCodeEnums responseCode;

    public BusinessException(ResponseCodeEnums responseCode) {
        super(responseCode.getMessage());
        this.responseCode = responseCode;
    }

    public BusinessException(ResponseCodeEnums responseCode, String customMessage) {
        super(customMessage);
        this.responseCode = responseCode;
    }

}