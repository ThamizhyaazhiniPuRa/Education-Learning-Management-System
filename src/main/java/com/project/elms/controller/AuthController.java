package com.project.elms.controller;

import com.project.elms.dto.*;
import com.project.elms.service.AuthService;
import com.project.elms.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;


@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    
    @Autowired
    private PasswordResetService passwordResetService;
    
    public AuthController(AuthService authService){ this.authService = authService; }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody SignupRequest req){
        authService.registerStudent(req);
        return ResponseEntity.ok("Registered");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req){
        return ResponseEntity.ok(authService.login(req));
    }
    
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
            }
            
            String token = passwordResetService.requestPasswordReset(email);
            return ResponseEntity.ok(Map.of(
                "message", "Reset code sent to your email",
                "token", token
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/verify-reset-code")
    public ResponseEntity<?> verifyResetCode(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String code = request.get("code");
            
            if (email == null || code == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email and code are required"));
            }
            
            boolean valid = passwordResetService.verifyResetCode(email, code);
            if (valid) {
                return ResponseEntity.ok(Map.of("valid", true, "message", "Code verified successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("valid", false, "error", "Invalid or expired code"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String code = request.get("code");
            String newPassword = request.get("newPassword");
            
            if (email == null || code == null || newPassword == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email, code, and new password are required"));
            }
            
            passwordResetService.resetPassword(email, code, newPassword);
            return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}