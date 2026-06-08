package com.project.elms.model;
 
import jakarta.persistence.*;
 
@Entity
@Table(name = "course")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer courseId; 
 
    @Column(length = 200, unique = true, nullable = false)
    private String title; 
 
    @Column(columnDefinition = "TEXT")
    private String description; 
 
    private Integer credits; 
 
    @Column(length = 20)
    private String semester; 

    @Column(name = "student_id")
    private Integer studentId;

    @Column(name = "instructor_id")
    private Integer instructorId;
 
    @Enumerated(EnumType.STRING)
    private CourseStatus status; 

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_status")
    private AssignmentStatus assignmentStatus;

    public enum CourseStatus 
    { ACTIVE, 
    	INACTIVE 
    }

    public enum AssignmentStatus {
        PENDING,
        ACCEPTED,
        REJECTED
    }
 
    public Course() {}
 
    
    public Integer getCourseId() 
    { 
    	return courseId; 
    }
    public void setCourseId(Integer courseId)
    { 
    	this.courseId = courseId;
    }
    public String getTitle() 
    { 
    	return title;
    }
    public void setTitle(String title)
    { 
    	this.title = title; 
    }
    public String getDescription()
    { return description;
    }
    public void setDescription(String description)
    { 
    	this.description = description;
    }
    public Integer getCredits() 
    {
    	return credits; 
    }
    public void setCredits(Integer credits)
    { 
    	this.credits = credits; 
    }
    public String getSemester() 
    { 
    	return semester; 
    }
    public void setSemester(String semester) 
    { 
    	this.semester = semester; 
    }
    public Integer getStudentId() 
    { 
    	return studentId; 
    }
    public void setStudentId(Integer studentId)
    { 
    	this.studentId = studentId;
    }
    public Integer getInstructorId()
    {
        return instructorId;
    }
    public void setInstructorId(Integer instructorId)
    {
        this.instructorId = instructorId;
    }
    public CourseStatus getStatus() 
    { 
    	return status;
    }
    public void setStatus(CourseStatus status) 
    { 
    	this.status = status;
    }

    public AssignmentStatus getAssignmentStatus() {
        return assignmentStatus;
    }
    public void setAssignmentStatus(AssignmentStatus assignmentStatus) {
        this.assignmentStatus = assignmentStatus;
    }
}