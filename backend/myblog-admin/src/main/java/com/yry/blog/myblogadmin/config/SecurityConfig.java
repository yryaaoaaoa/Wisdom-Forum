package com.yry.blog.myblogadmin.config;

import com.yry.blog.myblogadmin.jwt.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity

public class SecurityConfig {

    // JWT过滤器（通过构造器注入，确保Spring能正确加载）
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    // 自定义认证入口（如果没有这个类，注释掉相关配置）
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    // 构造器注入依赖（必须有，否则方法参数注入可能失效）
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          CustomAuthenticationEntryPoint customAuthenticationEntryPoint) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.customAuthenticationEntryPoint = customAuthenticationEntryPoint;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 核心：配置跨域（WebSocket必须加）
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 生产环境替换为具体前端域名
        config.setAllowedOriginPatterns(List.of("*"));
        // 允许所有请求头（包括Authorization）
        config.setAllowedHeaders(List.of("*"));
        // 允许所有HTTP方法
        config.setAllowedMethods(List.of("*"));
        // 允许携带凭证（前端跨域传Token需要）
        config.setAllowCredentials(true);
        // 暴露响应头
        config.setExposedHeaders(List.of("Authorization"));
        // 预检请求缓存时间
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有路径生效
        source.registerCorsConfiguration("/**", config);
        return source;
    }
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. 开启跨域（核心！WebSocket必须加）
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 2. 关闭CSRF（JWT场景必关，WebSocket也需要）
                .csrf(csrf -> csrf.disable())
                // 3. 无状态会话（JWT核心）
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // 4. 异常处理（保留，但放行路径会跳过）
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                )
                // 5. 添加JWT过滤器（顺序不能错）
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // 6. 授权规则（放行路径必须在anyRequest之前）
                .authorizeHttpRequests(auth -> auth
                        // 业务接口放行
                        .requestMatchers("/auth/login", "/auth/captcha","/api/users/add","/ws/notification/**").permitAll()
                        // 其他所有请求需要认证
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}