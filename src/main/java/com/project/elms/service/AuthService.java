package com.project.elms.service;

import com.project.elms.dto.AuthResponse;
import com.project.elms.dto.LoginRequest;
import com.project.elms.dto.SignupRequest;
import com.project.elms.model.Role;
import com.project.elms.model.Student;
import com.project.elms.repository.StudentRepository;
import com.project.elms.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final StudentRepository studentRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(StudentRepository studentRepo,
                       PasswordEncoder encoder,
                       JwtService jwt) {
        this.studentRepo = studentRepo;
        this.encoder = encoder;
        this.jwt = jwt;
    }
    

    @Transactional
    public void registerStudent(SignupRequest req) {
        try {
            String email = req.getEmail().trim().toLowerCase();
            String phone = req.getPhone().trim();

            if (studentRepo.existsByEmail(email)) {
                throw new IllegalArgumentException("Email already registered");
            }

            if (studentRepo.existsByPhone(phone)) {
                throw new IllegalArgumentException("Phone number already exists");
            }

            Student s = new Student();
            s.setName(req.getName().trim());
            s.setPhone(phone);
            s.setProgram(req.getProgram().trim());
            s.setEmail(email);
            s.setPasswordHash(encoder.encode(req.getPassword()));
            s.setRole(Role.STUDENT);
            s.setEnabled(true);
            s.setStatus(Student.StudentStatus.ACTIVE);

            Student saved = studentRepo.save(s);
            
            System.out.println("✓ Student SAVED: " + saved.getName() + " (ID: " + saved.getStudentId() + ")");
            System.out.println("  - Email: " + saved.getEmail());
            System.out.println("  - Role: " + saved.getRole());
            System.out.println("  - Status: " + saved.getStatus());
            System.out.println("  - Phone: " + saved.getPhone());
            System.out.println("  - Program: " + saved.getProgram());
        } catch (Exception e) {
            System.err.println("✗ REGISTRATION FAILED: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public AuthResponse login(LoginRequest req) {
        String email = req.getEmail().trim().toLowerCase();

        Student s = studentRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!s.isEnabled()) {
            throw new IllegalArgumentException("User disabled");
        }

        if (s.getRole() != req.getRole()) {
            throw new IllegalArgumentException("Invalid credentials / role");
        }

        if (!encoder.matches(req.getPassword(), s.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String token = jwt.generate(s.getEmail(), s.getRole().name());
        
        return new AuthResponse(token, s.getRole().name(), s.getName(), s.getStudentId(), s.getEmail(), s.getPhone(), s.getProgram());
    }
}