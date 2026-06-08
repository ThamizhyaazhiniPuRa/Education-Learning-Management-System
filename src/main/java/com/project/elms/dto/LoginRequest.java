package com.project.elms.dto;

import com.project.elms.model.Role;
import jakarta.validation.constraints.*;

public class LoginRequest {
    @NotBlank @Email private String email;
    @NotBlank private String password;
    @NotNull private Role role;

    public String getEmail(){return email;} public void setEmail(String email){this.email=email;}
    public String getPassword(){return password;} public void setPassword(String password){this.password=password;}
    public Role getRole(){return role;} public void setRole(Role role){this.role=role;}
}