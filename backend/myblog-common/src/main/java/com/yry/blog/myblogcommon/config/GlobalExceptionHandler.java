package com.yry.blog.myblogcommon.config;

import com.yry.blog.myblogcommon.enums.ResponseCodeEnums;
import com.yry.blog.myblogcommon.exception.BusinessException;
import com.yry.blog.myblogcommon.result.Response;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.NoSuchElementException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    private boolean isDev() {
        return "dev".equals(activeProfile);
    }

    @ExceptionHandler(BusinessException.class)
    public Response<String> handleBusinessException(BusinessException ex) {
        log.warn("业务异常: code={}, message={}", ex.getResponseCode().getCode(), ex.getMessage());
        return Response.error(ex.getResponseCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Response<String> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldError() != null 
                ? ex.getBindingResult().getFieldError().getDefaultMessage() 
                : "参数校验失败";
        log.warn("参数校验失败: {}", message);
        return Response.error(ResponseCodeEnums.BAD_REQUEST, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Response<String> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("约束违反: {}", ex.getMessage());
        return Response.error(ResponseCodeEnums.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Response<String> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("请求体解析失败: {}", ex.getMessage());
        String message = isDev() ? ex.getMessage() : "请求数据格式错误";
        return Response.error(ResponseCodeEnums.BAD_REQUEST, message);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Response<String> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("数据库约束冲突: {}", ex.getMessage());
        return Response.error(ResponseCodeEnums.DATA_EXISTS);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public Response<String> handleNoSuchElement(NoSuchElementException ex) {
        log.warn("资源未找到: {}", ex.getMessage());
        return Response.error(ResponseCodeEnums.NOT_FOUND);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Response<String> handleMissingParam(MissingServletRequestParameterException ex) {
        log.warn("缺少请求参数: {}", ex.getMessage());
        return Response.error(ResponseCodeEnums.BAD_REQUEST, "缺少必要参数: " + ex.getParameterName());
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Response<String>> handleAccessDenied() {
        log.warn("访问被拒绝");
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Response.error(ResponseCodeEnums.FORBIDDEN));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public Response<String> handleResponseStatus(ResponseStatusException ex) {
        log.warn("响应状态异常: status={}, reason={}", ex.getStatusCode(), ex.getReason());
        ResponseCodeEnums error = ResponseCodeEnums.getByCode(ex.getStatusCode().value());
        return error != null ? Response.error(error) : Response.error(ResponseCodeEnums.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Response<String> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("参数类型转换失败: {}，值: {}，期望类型: {}", ex.getName(), ex.getValue(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "未知");
        return Response.error(ResponseCodeEnums.BAD_REQUEST, "参数格式错误: " + ex.getValue());
    }

    @ExceptionHandler(Exception.class)
    public Response<String> handleException(Exception ex) {
        log.error("未处理的异常: ", ex);
        String message = isDev() ? ex.getClass().getSimpleName() + ": " + ex.getMessage() : "服务器内部错误";
        return Response.error(ResponseCodeEnums.INTERNAL_SERVER_ERROR, message);
    }
}
