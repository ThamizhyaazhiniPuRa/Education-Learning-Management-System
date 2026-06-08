package com.project.elms.service;
import com.project.elms.model.Course;
import com.project.elms.repository.AnalyticsRepository;
import com.project.elms.repository.CourseRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
@Service
public class CourseService {
 
    private final CourseRepository courseRepository;
    private final AnalyticsRepository analyticsRepository;
 
    public CourseService(CourseRepository courseRepository,AnalyticsRepository analyticsRepository) {
    	this.analyticsRepository = analyticsRepository;
        this.courseRepository = courseRepository;
    }
 
    public Course createCourse(Course course) {
        
        if (course.getStatus() == null) {
            course.setStatus(Course.CourseStatus.INACTIVE);
        }
        if (courseRepository.existsByTitle(course.getTitle())) {
            throw new IllegalArgumentException("Course name must be unique");
        }
        return courseRepository.save(course);
    }
    
    public void activateCourse(Integer id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        course.setStatus(Course.CourseStatus.ACTIVE);
        courseRepository.save(course);
    }

    public Course updateCourse(Integer id, Course details) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        if (!course.getTitle().equals(details.getTitle()) && courseRepository.existsByTitle(details.getTitle())) {
            throw new IllegalArgumentException("Course name must be unique");
        }
        course.setTitle(details.getTitle());
        course.setDescription(details.getDescription());
        course.setSemester(details.getSemester());
        course.setCredits(details.getCredits()); 
 
       
        if (details.getStatus() != null) {
            course.setStatus(details.getStatus());
        }
 
        return courseRepository.save(course);
    }
 
    public Optional<Course> getCourseDetails(Integer id) {
        return courseRepository.findById(id);
    }
 
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }
 
//    public void deleteCourse(Integer id) {
//        if (!courseRepository.existsById(id)) {
//            throw new RuntimeException("Course not found");
//        }
//        courseRepository.deleteById(id);
//    }
    @Transactional
    public void deleteCourse(Integer id) {
        if (!courseRepository.existsById(id)) {
            throw new RuntimeException("Course not found");
        }
       
        try {
            analyticsRepository.deleteByCourse_CourseId(id);
        } catch (Exception e) {
            System.err.println("Note: Clear snapshots encountered: " + e.getMessage());
        }
 
        courseRepository.deleteById(id);
    }
    public List<Course> searchCourses(String keyword) {
        return courseRepository.findByTitleContainingIgnoreCase(keyword);
    }
 
    public List<Course> getCoursesByStudentId(Integer studentId) {
        return courseRepository.findByStudentId(studentId);
    }
 
    public List<Course> getCoursesByInstructorId(Integer instructorId) {
        return courseRepository.findByInstructorId(instructorId);
    }
 
    public void assignInstructor(Integer courseId, Integer instructorId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        if (course.getInstructorId() != null && course.getInstructorId().equals(instructorId)) {
            throw new IllegalArgumentException("Instructor is already assigned to this course.");
        }
        course.setInstructorId(instructorId);
        course.setAssignmentStatus(Course.AssignmentStatus.PENDING);
        courseRepository.save(course);
    }
 
    public void respondToAssignment(Integer courseId, Integer instructorId, boolean accepted) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        if (!instructorId.equals(course.getInstructorId())) {
            throw new RuntimeException("You are not assigned to this course");
        }
        if (course.getAssignmentStatus() != Course.AssignmentStatus.PENDING) {
            throw new RuntimeException("Assignment already responded to");
        }
        course.setAssignmentStatus(accepted ? Course.AssignmentStatus.ACCEPTED : Course.AssignmentStatus.REJECTED);
        courseRepository.save(course);
    }
 
    public List<Course> getCoursesByAssignmentStatus(Course.AssignmentStatus status) {
        return courseRepository.findByAssignmentStatus(status);
    }
 
    public List<Course> getAllCoursesWithAssignment() {
        return courseRepository.findAll().stream()
                .filter(c -> c.getAssignmentStatus() != null)
                .collect(Collectors.toList());
    }
}