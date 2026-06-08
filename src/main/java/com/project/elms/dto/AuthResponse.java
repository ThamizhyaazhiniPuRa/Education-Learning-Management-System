package com.project.elms.dto;

public class AuthResponse {
    private String token;
    private String role;
    private String name;
    private Integer studentId;
    private String email;
    private String phone;
    private String program;

    public AuthResponse(String token, String role, String name) {
        this(token, role, name, null, null, null, null);
    }

    public AuthResponse(String token, String role, String name, Integer studentId, String email, String phone, String program) {
        this.token = token;
        this.role = role;
        this.name = name;
        this.studentId = studentId;
        this.email = email;
        this.phone = phone;
        this.program = program;
    }

    public String getToken() { return token; }
    public String getRole() { return role; }
    public String getName() { return name; }
    public Integer getStudentId() { return studentId; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getProgram() { return program; }
}