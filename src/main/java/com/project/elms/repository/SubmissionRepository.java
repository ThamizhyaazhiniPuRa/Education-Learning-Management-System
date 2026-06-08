package com.project.elms.repository;

import com.project.elms.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    
    List<Submission> findByAssignment_Course_CourseId(Integer courseId);
    List<Submission> findByAssignment_AssignmentId(Long assignmentId);
    List<Submission> findByStudent_StudentId(Integer studentId);
    boolean existsByAssignment_AssignmentIdAndStudent_StudentId(Long assignmentId, Integer studentId);
    java.util.Optional<Submission> findByAssignment_AssignmentIdAndStudent_StudentId(Long assignmentId, Integer studentId);

    /** Count all attempts (original + retakes) for a student on one assignment */
    long countByAssignment_AssignmentIdAndStudent_StudentId(Long assignmentId, Integer studentId);

    /** Latest submission by retakeCount for showing best/latest attempt */
    java.util.Optional<Submission> findTopByAssignment_AssignmentIdAndStudent_StudentIdOrderByRetakeCountDesc(Long assignmentId, Integer studentId);
}