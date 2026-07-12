package com.yry.blog.myblogwebsocket.config;

import com.yry.blog.myblogauth.jwt.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import java.util.List;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtils jwtUtil;

    public WebSocketConfig(JwtUtils jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    // 配置消息代理
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue"); // 广播/点对点消息前缀
        config.setApplicationDestinationPrefixes("/app"); // 前端发消息的前缀
        config.setUserDestinationPrefix("/user"); // 点对点用户前缀
    }

    // 注册STOMP端点（去掉SockJS）
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/notification") // 原生WebSocket端点
                .setAllowedOriginPatterns("*"); // 允许跨域（生产环境指定具体域名）
        // 去掉握手拦截器，改在通道拦截器里处理Token
    }

    // 核心修改：在通道拦截器中处理STOMP CONNECT帧的Token
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                // 只处理CONNECT命令
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // 1. 从STOMP帧头获取Authorization Token
                    List<String> authHeaders = accessor.getNativeHeader("Authorization");
                    String token = null;
                    if (authHeaders != null && !authHeaders.isEmpty()) {
                        token = authHeaders.get(0);
                    }

                    // 2. 校验Token
                    if (token != null && token.startsWith("Bearer ")) {
                        try {
                            token = token.substring(7).trim();
                            Long userId = jwtUtil.getUserIdFromToken(token);

                            if (userId != null) {
                                // 3. 绑定用户信息到会话和SecurityContext
                                accessor.getSessionAttributes().put("userId", userId.toString());
                                Authentication auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
                                SecurityContextHolder.getContext().setAuthentication(auth);
                                accessor.setUser(auth);
                                log.debug("STOMP CONNECT成功，用户ID：{}", userId);
                            } else {
                                // Token无效，拒绝连接
                                accessor.setLeaveMutable(true);
                                log.warn("Token无效，用户ID为空");
                            }
                        } catch (Exception e) {
                            // Token解析失败，拒绝连接
                            accessor.setLeaveMutable(true);
                            log.warn("Token验证失败：{}", e.getMessage());
                        }
                    } else {
                        // 未携带Token，拒绝连接
                        accessor.setLeaveMutable(true);
                        log.warn("未携带Authorization Token");
                    }
                }
                return message;
            }
        });
    }
}