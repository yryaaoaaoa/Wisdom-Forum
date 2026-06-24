package com.yry.blog.myblogadmin.service;

import com.yry.blog.myblogadmin.dto.ResetPasswordDTO;

public interface PasswordResetService {
    
    String createResetToken(String email);
    
    boolean validateToken(String token);
    
    String getEmailByToken(String token);
    
    void resetPassword(String token, String newPassword);
    
    void sendResetEmail(String username, String email);
}
