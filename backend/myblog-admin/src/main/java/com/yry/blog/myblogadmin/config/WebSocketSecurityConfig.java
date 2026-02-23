//package com.yry.blog.myblogadmin.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.messaging.Message;
//import org.springframework.messaging.simp.SimpMessageType;
//import org.springframework.security.authorization.AuthorizationManager;
//import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
//import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;
//
//@Configuration
//@EnableWebSocketSecurity
//public class WebSocketSecurityConfig {
//
//    @Bean
//    AuthorizationManager<Message<?>> messageAuthorizationManager(MessageMatcherDelegatingAuthorizationManager.Builder messages) {
//        messages
//                .simpTypeMatchers(SimpMessageType.CONNECT, SimpMessageType.DISCONNECT) // CONNECT 和 DISCONNECT
//                .authenticated() // 需要认证
//                .simpDestMatchers("/topic/**", "/queue/**") // /topic/** 和 /queue/**
//                .authenticated() // 需要认证
//                .anyMessage() // 其他消息
//                .denyAll(); // 拒绝
//
//        return messages.build();
//    }
//}