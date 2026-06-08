package com.project.elms.service;
 
import com.project.elms.model.Assignment;
import com.project.elms.model.Course;
import com.project.elms.model.CourseEnrollment;
import com.project.elms.model.Student;
import com.project.elms.model.Student.StudentStatus;
import com.project.elms.model.Role;
import com.project.elms.model.Submission;
import com.project.elms.repository.AssignmentRepository;
import com.project.elms.repository.CourseEnrollmentRepository;
import com.project.elms.repository.CourseRepository;
import com.project.elms.repository.StudentRepository;
import com.project.elms.repository.SubmissionRepository;
import com.project.elms.dto.StudentSummary;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
 
@Service
public class StudentService {
    @Autowired
    private AssignmentRepository assignmentRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private CourseEnrollmentRepository courseEnrollmentRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private SubmissionRepository submissionRepository;

    public Student registerStudent(Student student) {
        student.setStatus(StudentStatus.ACTIVE);
        return studentRepository.save(student);
    }

    public void enrollCourse(Integer studentId, Integer courseId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        // Insert into enrollment table (many-to-many) only if not already enrolled
        if (!courseEnrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            courseEnrollmentRepository.save(new CourseEnrollment(studentId, courseId));
        }

        // Activate the course
        course.setStatus(Course.CourseStatus.ACTIVE);
        courseRepository.save(course);

        // Ensure student is ACTIVE when enrolling (re-activate if previously DROPPED)
        student.setStatus(StudentStatus.ACTIVE);
        studentRepository.save(student);
    }

    /**
     * Central status evaluator.
     * Rules:
     *  1. DROPPED  — If ANY assignment is overdue (past due date) AND student hasn't passed it (score < 50 or no submission).
     *  2. GRADUATED — Student passed (score >= 50) EVERY assignment in EVERY enrolled course AND no pending assignments.
     *  3. ACTIVE   — everything else (enrolled, some assignments pending but not overdue).
     */
    public StudentStatus evaluateStudentStatus(Integer studentId) {
        Student student = studentRepository.findById(studentId).orElse(null);
        if (student == null) return null;

        List<CourseEnrollment> enrollments = courseEnrollmentRepository.findByStudentId(studentId);
        if (enrollments.isEmpty()) return StudentStatus.ACTIVE;

        LocalDate today = LocalDate.now();
        boolean hasAnyOverdueUnpassed = false;
        boolean allAssignmentsPassed = true;
        boolean hasAnyAssignment = false;

        for (CourseEnrollment enrollment : enrollments) {
            Integer courseId = enrollment.getCourseId();
            List<Assignment> assignments = assignmentRepository.findByCourse_CourseId(courseId);
            
            for (Assignment a : assignments) {
                hasAnyAssignment = true;
                
                LocalDate dueDate = a.getDueDate() != null
                        ? a.getDueDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                        : today.plusDays(365); // No due date means far future

                Optional<Submission> bestSubmission = submissionRepository
                        .findTopByAssignment_AssignmentIdAndStudent_StudentIdOrderByRetakeCountDesc(
                                a.getAssignmentId(), studentId);
                
                boolean passed = bestSubmission.isPresent()
                        && bestSubmission.get().getScore() != null
                        && bestSubmission.get().getScore() >= 50;

                // Check if this assignment is overdue (past due date) and not passed
                boolean isOverdue = today.isAfter(dueDate);
                
                if (isOverdue && !passed) {
                    // Any single overdue unpassed assignment → DROPPED
                    hasAnyOverdueUnpassed = true;
                }
                
                if (!passed) {
                    allAssignmentsPassed = false;
                }
            }
        }

        // Priority: DROPPED > GRADUATED > ACTIVE
        if (hasAnyOverdueUnpassed) {
            return StudentStatus.DROPPED;
        }
        
        // GRADUATED: All enrolled courses have assignments AND all are passed
        if (hasAnyAssignment && allAssignmentsPassed) {
            return StudentStatus.GRADUATED;
        }
        
        // Default: ACTIVE (enrolled, working on assignments, none overdue)
        return StudentStatus.ACTIVE;
    }

    /** Re-evaluates and saves a student's status. */
    public StudentStatus refreshStudentStatus(Integer studentId) {
        Student student = studentRepository.findById(studentId).orElse(null);
        if (student == null) return null;
        StudentStatus newStatus = evaluateStudentStatus(studentId);
        if (newStatus == null) return null;
        student.setStatus(newStatus);
        studentRepository.save(student);
        return newStatus;
    }

    /** Batch re-evaluates all students enrolled in a course (called after due-date extension). */
    public void refreshAllStudentsInCourse(Integer courseId) {
        List<CourseEnrollment> enrollments = courseEnrollmentRepository.findByCourseId(courseId);
        for (CourseEnrollment e : enrollments) {
            try { refreshStudentStatus(e.getStudentId()); } catch (Exception ignored) {}
        }
    }

    /**
     * Manually mark student as DROPPED (admin/system action).
     * Checks if any past-due unsubmitted assignment exists; otherwise no-op.
     */
    public void markAsDropped(Integer id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // Force DROPPED regardless of evaluation (manual admin action)
        student.setStatus(StudentStatus.DROPPED);
        studentRepository.save(student);
    }

    /**
     * Mark as GRADUATED only if not already DROPPED.
     */
    public void markAsGraduated(Integer id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        if (student.getStatus() == StudentStatus.DROPPED) {
            throw new RuntimeException("A DROPPED student cannot be graduated.");
        }
        student.setStatus(StudentStatus.GRADUATED);
        studentRepository.save(student);
    }
 
    public Student updateProfile(Integer id, Student details) {
        Student student = studentRepository.findById(id).orElseThrow();
        student.setName(details.getName());
        student.setPhone(details.getPhone());
        student.setProgram(details.getProgram());
        return studentRepository.save(student);
    }
 
    public Optional<Student> getStudentDetails(Integer id) {
        return studentRepository.findById(id);
    }
    
    public List<Student> getAllStudents() {
        return studentRepository.findAllWithUser().stream()
                .filter(s -> s.getRole() == Role.STUDENT)
                .collect(Collectors.toList());
    }

    public List<StudentSummary> getAllStudentSummaries() {
        return studentRepository.findAllWithUser().stream()
                .filter(s -> s.getRole() == Role.STUDENT)
                .map(student -> new StudentSummary(
                        student.getStudentId(),
                        student.getName(),
                        student.getEmail(),
                        student.getPhone(),
                        student.getProgram(),
                        student.getStatus() != null ? student.getStatus().name() : null
                ))
                .collect(Collectors.toList());
    }

    public Student getStudentById(Long id) {
        return studentRepository.findById(id.intValue())
                .orElseThrow(() -> new RuntimeException("Student not found"));
    }
}