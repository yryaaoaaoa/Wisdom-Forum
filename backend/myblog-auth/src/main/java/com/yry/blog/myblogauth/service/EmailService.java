package com.yry.blog.myblogauth.service;

public interface EmailService {
    
    void sendPasswordResetEmail(String toEmail, String resetToken, String resetUrl);
    
    void sendSimpleEmail(String toEmail, String subject, String content);
}
