package com.yry.blog.myblogauth.service;

import com.yry.blog.myblogauth.dto.ResetPasswordDTO;

public interface PasswordResetService {
    
    String createResetToken(String email);
    
    boolean validateToken(String token);
    
    String getEmailByToken(String token);
    
    void resetPassword(String token, String newPassword);
    
    void sendResetEmail(String username, String email);
}
