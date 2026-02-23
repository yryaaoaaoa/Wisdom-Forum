package com.yry.blog.myblogcommon.exception;

public class MinioException extends RuntimeException {
  public MinioException(String message, Exception e) {
    super(message);
  }
}
