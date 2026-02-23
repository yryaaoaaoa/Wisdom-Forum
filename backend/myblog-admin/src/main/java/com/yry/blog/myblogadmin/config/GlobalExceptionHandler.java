//package com.yry.blog.myblogadmin.config; // 请取消注释包声明
//
//import com.yry.blog.myblogcommon.enums.ResponseCodeEnums;
//import com.yry.blog.myblogcommon.exception.BusinessException;
//import com.yry.blog.myblogcommon.result.Response;
//import io.minio.errors.MinioException;
//import jakarta.validation.ConstraintViolationException;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory; // 引入 SLF4J 日志
//import org.springframework.context.annotation.Profile;
//import org.springframework.dao.DataIntegrityViolationException;
//import org.springframework.http.converter.HttpMessageNotReadableException; // 确保导入
//import org.springframework.web.bind.MethodArgumentNotValidException;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.bind.annotation.RestControllerAdvice;
//import org.springframework.web.server.ResponseStatusException;
//
//import static com.yry.blog.myblogcommon.enums.ResponseCodeEnums.METHOD_NOT_ALLOWED;
//
//import java.util.NoSuchElementException; // 确保导入正确的 NotFoundException
//
///**
// * 全局异常处理器，统一返回 Response 格式
// */
//
//
//@RestControllerAdvice
//public class GlobalExceptionHandler {
//
//    // 创建一个 Logger 实例
//    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
//
//    /**
//     * 处理 MinIO 操作异常
//     */
//    @ExceptionHandler(MinioException.class)
//    public Response<String> handleMinioException(MinioException ex) {
//        logger.error("MinIO 异常: ", ex); // 使用 logger 记录错误
//        return Response.error(ResponseCodeEnums.INTERNAL_SERVER_ERROR);
//    }
//
//    /**
//     * 新增：专门处理 JSON 格式错误或请求体不可读异常
//     * 给前端返回详细错误信息，后端记录日志
//     */
//    @ExceptionHandler(HttpMessageNotReadableException.class)
//    public Response<String> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
//        // 1. 后端记录详细日志
//        logger.warn("请求体数据无法读取或JSON格式错误: ", ex); // 记录完整堆栈
//        // 或者只记录消息: logger.warn("请求体数据无法读取或JSON格式错误: {}", ex.getMessage());
//
//        // 2. 构造返回给前端的详细错误信息
//        String errorMessage = "请求数据格式错误";
//        // 尝试获取更具体的错误原因
//        if (ex.getRootCause() != null) {
//            errorMessage += ": " + ex.getRootCause().getMessage();
//        } else if (ex.getMessage() != null) {
//            // 如果没有 root cause，尝试用 getMessage
//            errorMessage += ": " + ex.getMessage();
//        }
//
//        // 3. 返回 400 Bad Request 和详细信息
//        return Response.error(ResponseCodeEnums.BAD_REQUEST, errorMessage);
//    }
//
//
//    /**
//     * 修改：处理其他参数校验异常（移除了 HttpMessageNotReadableException）
//     * 例如 @Valid 失败, ConstraintViolationException, IllegalArgumentException
//     */
//    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class, IllegalArgumentException.class})
//    public Response<String> handleOtherValidationExceptions(Exception ex) { // 修改参数类型以匹配所有列出的异常
//        logger.warn("参数校验失败: {}", ex.getMessage()); // 可选：记录日志
//        return Response.error(ResponseCodeEnums.BAD_REQUEST);
//    }
//
//    /**
//     * 处理数据库唯一约束冲突（如插入重复用户名）
//     */
//    @ExceptionHandler(DataIntegrityViolationException.class)
//    public Response<String> handleDataIntegrityViolation() {
//        logger.warn("数据库约束冲突"); // 可选：记录日志
//        return Response.error(ResponseCodeEnums.DATA_EXISTS);
//    }
//
//    /**
//     * 处理资源未找到异常
//     * 注意：检查导入的类是否正确，Spring 通常使用 org.springframework.web.server.ResponseStatusException
//     * 或者你的业务逻辑抛出的特定 NotFoundException
//     */
//    @ExceptionHandler({org.springframework.data.crossstore.ChangeSetPersister.NotFoundException.class, NoSuchElementException.class}) // 确保导入正确
//    public Response<String> handleResourceNotFoundException() {
//        // 可选：记录日志 logger.info("资源未找到");
//        return Response.error(ResponseCodeEnums.NOT_FOUND);
//    }
//
//    /**
//     * 处理权限不足异常
//     */
//    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
//    public Response<String> handleAccessDeniedException() {
//        // 可选：记录日志 logger.warn("访问被拒绝");
//        return Response.error(ResponseCodeEnums.FORBIDDEN);
//    }
//
//    /**
//     * 处理不支持的 HTTP 方法
//     */
//    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
//    public Response<String> handleMethodNotSupportedException() {
//        // 可选：记录日志 logger.warn("不支持的 HTTP 方法");
//        return Response.error(METHOD_NOT_ALLOWED);
//    }
//
//    @ExceptionHandler(NullPointerException.class)
//    public Response<String> handleNullPointerException(NullPointerException ex) { // 添加参数
//        logger.error("发生空指针异常: ", ex); // 记录详细错误
//        return Response.error(ResponseCodeEnums.BAD_REQUEST); // 或自定义错误码
//    }
//
//    @ExceptionHandler(BusinessException.class)
//    public Response<String> handleBusinessException(BusinessException ex) {
//        // BusinessException 通常自己携带了错误码和信息，可以直接用
//        logger.warn("业务异常: code={}, message={}", ex.getResponseCode().getCode(), ex.getMessage()); // 记录日志
//        ResponseCodeEnums error = ex.getResponseCode();
//        return Response.error(error);
//    }
//
//    /**
//     * 处理所有未捕获的异常，默认返回 500
//     * 注意：因为这个是 Exception.class，它应该放在最后，作为兜底。
//     * 并且 BusinessException, ResponseStatusException 等都会被它捕获，除非它们有更具体的处理器（如上所示）。
//     */
//    @ExceptionHandler(Exception.class)
//    public Response<String> handleAllExceptions(Exception ex) {
//        // 检查是否是已知的 ResponseStatusException
//        if (ex instanceof ResponseStatusException statusEx) {
//            int statusCode = statusEx.getStatusCode().value();
//            ResponseCodeEnums error = ResponseCodeEnums.getByCode(statusCode);
//            logger.warn("ResponseStatusException: status={}, reason={}", statusCode, statusEx.getReason()); // 记录日志
//            // 如果 getByCode 没找到对应枚举，error 可能为 null，需要处理
//            if (error != null) {
//                return Response.error(error);
//            } else {
//                logger.error("未知的 ResponseStatusException 状态码: {}", statusCode);
//                return Response.error(ResponseCodeEnums.INTERNAL_SERVER_ERROR);
//            }
//        }
//        // 对于其他所有未被捕获的异常，记录详细错误并返回 500
//        logger.error("未处理的服务器内部错误: ", ex); // 记录完整堆栈
//        return Response.error(ResponseCodeEnums.INTERNAL_SERVER_ERROR);
//    }
//
//}