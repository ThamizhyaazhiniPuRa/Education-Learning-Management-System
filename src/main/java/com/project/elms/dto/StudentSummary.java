package com.project.elms.dto;

public class StudentSummary {
    private Integer studentId;
    private String name;
    private String email;
    private String phone;
    private String program;
    private String status;

    public StudentSummary(Integer studentId, String name, String email, String phone, String program, String status) {
        this.studentId = studentId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.program = program;
        this.status = status;
    }

    public Integer getStudentId() { return studentId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getProgram() { return program; }
    public String getStatus() { return status; }
}
