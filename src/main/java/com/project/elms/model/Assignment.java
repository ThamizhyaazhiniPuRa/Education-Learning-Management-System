package com.project.elms.model;
 
import jakarta.persistence.*;
import java.util.Date;
 
@Entity
@Table(name = "Assignment")
public class Assignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long assignmentId; 
 
    @ManyToOne
    @JoinColumn(name = "courseId") 
    private Course course;
 
    private String title; 
    private Date dueDate; 
    private Integer maxScore; 
    @Column(columnDefinition = "TEXT")
    private String description;
  
    public Assignment() {}
 
    
    public Long getAssignmentId() { return assignmentId; }
    public void setAssignmentId(Long assignmentId) { this.assignmentId = assignmentId; }
    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Date getDueDate() { return dueDate; }
    public void setDueDate(Date dueDate) { this.dueDate = dueDate; }
    public Integer getMaxScore() { return maxScore; }
    public void setMaxScore(Integer maxScore) { this.maxScore = maxScore; }// Add Getter and Setter
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}