package com.project.elms.repository;

import com.project.elms.model.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    
    List<Assignment> findByCourse_CourseId(Long courseId);
   
    List<Assignment> findByCourse_CourseId(Integer courseId);
    @Query(value = "SELECT a.* FROM assignment a " +
                   "JOIN course_enrollment ce ON a.course_id = ce.course_id " +
                   "WHERE ce.student_id = :studentId", 
           nativeQuery = true)
    List<Assignment> findAssignmentsByStudentId(@Param("studentId") Integer studentId);
}