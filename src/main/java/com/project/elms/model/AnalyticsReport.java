package com.project.elms.model;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "analytics_report")
public class AnalyticsReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String reportType; // OVERVIEW, COURSE, STUDENT

    @Column(columnDefinition = "TEXT")
    private String reportData; // JSON or CSV data

    @Column(nullable = false)
    private Date generatedAt;

    private String generatedBy; // Admin name/email

    private Integer avgScore;
    private Integer attendanceRate;
    private Integer totalSubmissions;
    private Integer totalCourses;
    private Integer healthIndex;

    public AnalyticsReport() {
        this.generatedAt = new Date();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }

    public String getReportData() { return reportData; }
    public void setReportData(String reportData) { this.reportData = reportData; }

    public Date getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Date generatedAt) { this.generatedAt = generatedAt; }

    public String getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(String generatedBy) { this.generatedBy = generatedBy; }

    public Integer getAvgScore() { return avgScore; }
    public void setAvgScore(Integer avgScore) { this.avgScore = avgScore; }

    public Integer getAttendanceRate() { return attendanceRate; }
    public void setAttendanceRate(Integer attendanceRate) { this.attendanceRate = attendanceRate; }

    public Integer getTotalSubmissions() { return totalSubmissions; }
    public void setTotalSubmissions(Integer totalSubmissions) { this.totalSubmissions = totalSubmissions; }

    public Integer getTotalCourses() { return totalCourses; }
    public void setTotalCourses(Integer totalCourses) { this.totalCourses = totalCourses; }

    public Integer getHealthIndex() { return healthIndex; }
    public void setHealthIndex(Integer healthIndex) { this.healthIndex = healthIndex; }
}
