

package com.project.elms.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "student",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_student_phone", columnNames = "phone"),
        @UniqueConstraint(name = "uk_student_email", columnNames = "email")
    }
)
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "student_id")
    private Integer studentId;

    @Column(name = "name", length = 200, nullable = false)
    private String name;

    @Column(name = "phone", length = 10, nullable = false, unique = true)
    private String phone;

    @Column(name = "program", length = 100, nullable = false)
    private String program;

    @Column(name = "email", length = 150, unique = true, nullable = false)
    private String email;

    @JsonIgnore
    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role = Role.STUDENT;

    @Column(nullable = false)
    private boolean enabled = true;

    // Courses this student is enrolled in (course.student_id → this student)
    // insertable/updatable=false: ownership of the FK lives on the Course side only
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", insertable = false, updatable = false)
    private List<Course> courses = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StudentStatus status = StudentStatus.ACTIVE;

    public enum StudentStatus { ACTIVE, GRADUATED, DROPPED }

    public Integer getStudentId() { return studentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name == null ? null : name.trim(); }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone == null ? null : phone.trim(); }

    public String getProgram() { return program; }
    public void setProgram(String program) { this.program = program == null ? null : program.trim(); }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email == null ? null : email.trim().toLowerCase(); }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public List<Course> getCourses() { return courses; }
    public void setCourses(List<Course> courses) { this.courses = courses; }

    public StudentStatus getStatus() { return status; }
    public void setStatus(StudentStatus status) { this.status = status; }
}