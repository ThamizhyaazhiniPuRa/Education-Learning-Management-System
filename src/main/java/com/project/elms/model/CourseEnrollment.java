package com.project.elms.model;

import jakarta.persistence.*;
import java.util.Date;

/**
 * Join table entity for the many-to-many relationship between Student and Course.
 * Each row represents one student enrolled in one course.
 * This replaces the broken single student_id FK on the course table.
 */
@Entity
@Table(
    name = "course_enrollment",
    uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "course_id"})
)
public class CourseEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @Column(name = "course_id", nullable = false)
    private Integer courseId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "enrolled_at")
    private Date enrolledAt = new Date();

    public CourseEnrollment() {}

    public CourseEnrollment(Integer studentId, Integer courseId) {
        this.studentId = studentId;
        this.courseId = courseId;
        this.enrolledAt = new Date();
    }

    public Integer getId() { return id; }

    public Integer getStudentId() { return studentId; }
    public void setStudentId(Integer studentId) { this.studentId = studentId; }

    public Integer getCourseId() { return courseId; }
    public void setCourseId(Integer courseId) { this.courseId = courseId; }

    public Date getEnrolledAt() { return enrolledAt; }
    public void setEnrolledAt(Date enrolledAt) { this.enrolledAt = enrolledAt; }
}
