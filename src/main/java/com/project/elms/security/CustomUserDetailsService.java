package com.project.elms.security;

import com.project.elms.model.Student;
import com.project.elms.repository.StudentRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final StudentRepository repo;
    public CustomUserDetailsService(StudentRepository repo){ this.repo = repo; }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Student s = repo.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new org.springframework.security.core.userdetails.User(
                s.getEmail(), s.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + s.getRole().name()))
        );
    }
}