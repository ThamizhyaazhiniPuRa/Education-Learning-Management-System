package com.project.elms.model;
 
import jakarta.persistence.*;
import java.util.Date;
 
@Entity
@Table(name = "Submission")
public class Submission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long submissionId;
 
    @ManyToOne
    @JoinColumn(name = "assignmentId") 
    private Assignment assignment;
 
    @ManyToOne
    @JoinColumn(name = "studentId") 
    private Student student;
 
    private Date submittedOn; 
    private Integer score; 
    private String feedback; 

    @Column(columnDefinition = "TEXT")
    private String answer;

    @Column(name = "retake_count", nullable = false)
    private int retakeCount = 0;
 
    public Submission() {}
 
    public Long getSubmissionId() { return submissionId; }
    public void setSubmissionId(Long submissionId) { this.submissionId = submissionId; }
    public Assignment getAssignment() { return assignment; }
    public void setAssignment(Assignment assignment) { this.assignment = assignment; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public Date getSubmittedOn() { return submittedOn; }
    public void setSubmittedOn(Date submittedOn) { this.submittedOn = submittedOn; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public int getRetakeCount() { return retakeCount; }
    public void setRetakeCount(int retakeCount) { this.retakeCount = retakeCount; }
}