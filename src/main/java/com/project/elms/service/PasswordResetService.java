package com.project.elms.service;

import com.project.elms.model.PasswordResetToken;
import com.project.elms.model.Role;
import com.project.elms.model.Student;
import com.project.elms.repository.PasswordResetTokenRepository;
import com.project.elms.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    @Autowired
    private PasswordResetTokenRepository tokenRepository;
    
    @Autowired
    private StudentRepository studentRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JavaMailSender mailSender;
    
    private static final int CODE_EXPIRY_MINUTES = 15;
    private static final SecureRandom random = new SecureRandom();
    
    /**
     * Generate a 6-digit reset code
     */
    private String generateResetCode() {
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
    
    /**
     * Request password reset - sends code to email
     */
    @Transactional
    public String requestPasswordReset(String email) {
        email = email.trim().toLowerCase();
        
        // Check if user exists and is a student
        Optional<Student> studentOpt = studentRepository.findByEmail(email);
        if (studentOpt.isEmpty()) {
            throw new IllegalArgumentException("No account found with this email address");
        }
        
        Student student = studentOpt.get();
        if (student.getRole() != Role.STUDENT) {
            throw new IllegalArgumentException("Password reset is only available for students");
        }
        
        // Delete any existing tokens for this email
        tokenRepository.deleteByEmail(email);
        
        // Generate new token and code
        String token = UUID.randomUUID().toString();
        String resetCode = generateResetCode();
        
        PasswordResetToken resetToken = new PasswordResetToken(token, email, resetCode, CODE_EXPIRY_MINUTES);
        tokenRepository.save(resetToken);
        
        // Send email to the student's registered email
        sendResetCodeEmail(email, student.getName(), resetCode);
        
        return token;
    }
    
    /**
     * Send reset code via email to the student
     */
    private void sendResetCodeEmail(String toEmail, String name, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("ELMS - Password Reset Code");
            message.setText(
                "Hello " + name + ",\n\n" +
                "You requested a password reset for your ELMS account.\n\n" +
                "Your password reset code is: " + code + "\n\n" +
                "This code will expire in " + CODE_EXPIRY_MINUTES + " minutes.\n\n" +
                "If you did not request this, please ignore this email.\n\n" +
                "Best regards,\n" +
                "ELMS Team"
            );
            
            mailSender.send(message);
            System.out.println("✓ Password reset code sent to: " + toEmail);
            
        } catch (Exception e) {
            System.err.println("✗ Failed to send email to " + toEmail + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send reset code email. Please try again later.");
        }
    }
    
    /**
     * Verify reset code
     */
    public boolean verifyResetCode(String email, String code) {
        email = email.trim().toLowerCase();
        
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByEmailAndResetCodeAndUsedFalse(email, code);
        if (tokenOpt.isEmpty()) {
            return false;
        }
        
        PasswordResetToken token = tokenOpt.get();
        return !token.isExpired();
    }
    
    /**
     * Reset password with code verification
     */
    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        email = email.trim().toLowerCase();
        
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByEmailAndResetCodeAndUsedFalse(email, code);
        if (tokenOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid or expired reset code");
        }
        
        PasswordResetToken token = tokenOpt.get();
        if (token.isExpired()) {
            throw new IllegalArgumentException("Reset code has expired. Please request a new one.");
        }
        
        // Validate new password
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long");
        }
        
        // Get and update student password
        Student student = studentRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        
        student.setPasswordHash(passwordEncoder.encode(newPassword));
        studentRepository.save(student);
        
        // Mark token as used
        token.setUsed(true);
        tokenRepository.save(token);
        
        System.out.println("✓ Password reset successful for: " + email);
    }
    
    /**
     * Clean up expired tokens (can be called by a scheduler)
     */
    @Transactional
    public void cleanupExpiredTokens() {
        tokenRepository.deleteExpiredTokens(java.time.LocalDateTime.now());
    }
}