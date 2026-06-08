package com.project.elms.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "AnalyticsSnapshot")
public class AnalyticsSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer reportId;

    @ManyToOne
    @JoinColumn(name = "courseId") 
    private Course course;

    private Date generatedOn; 
    private BigDecimal averageScore; 
    private BigDecimal attendanceRate; 
    private BigDecimal engagementIndex;

    public AnalyticsSnapshot() {}

   
    public Integer getReportId() { return reportId; }
    public void setReportId(Integer reportId) { this.reportId = reportId; }
    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
    public Date getGeneratedOn() { return generatedOn; }
    public void setGeneratedOn(Date generatedOn) { this.generatedOn = generatedOn; }
    public BigDecimal getAverageScore() { return averageScore; }
    public void setAverageScore(BigDecimal averageScore) { this.averageScore = averageScore; }
    public BigDecimal getAttendanceRate() { return attendanceRate; }
    public void setAttendanceRate(BigDecimal attendanceRate) { this.attendanceRate = attendanceRate; }
    public BigDecimal getEngagementIndex() { return engagementIndex; }
    public void setEngagementIndex(BigDecimal engagementIndex) { this.engagementIndex = engagementIndex; }
}