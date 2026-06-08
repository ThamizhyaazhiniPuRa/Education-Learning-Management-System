package com.project.elms.model;
 
import jakarta.persistence.*;
import java.util.Date;
 
@Entity
@Table(name = "Attendance")
public class Attendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer attendanceId; 

    @ManyToOne
    @JoinColumn(name = "studentId")
    private Student student;
 
    @ManyToOne
    @JoinColumn(name = "courseId") 
    private Course course;
 
    private Date date; 
 
    @Enumerated(EnumType.STRING)
    private AttendanceStatus status;
 
    public enum AttendanceStatus { PRESENT, ABSENT }
 
    public Attendance() {}

    public Integer getAttendanceId() { return attendanceId; }
    public void setAttendanceId(Integer attendanceId) { this.attendanceId = attendanceId; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }
    public AttendanceStatus getStatus() { return status; }
    public void setStatus(AttendanceStatus status) { this.status = status; }
}