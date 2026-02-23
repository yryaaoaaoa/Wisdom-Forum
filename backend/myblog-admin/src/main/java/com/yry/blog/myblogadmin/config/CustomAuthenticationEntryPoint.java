package com.yry.blog.myblogadmin.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        // 设置响应格式为JSON
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401状态码
        // 自定义401返回体，避免默认的HTML响应
        Map<String, Object> result = new HashMap<>();
        result.put("code", 401);
        result.put("msg", "未认证，请先登录");
        result.put("data", null);
        // 写入响应
        response.getWriter().write(new ObjectMapper().writeValueAsString(result));
    }
}