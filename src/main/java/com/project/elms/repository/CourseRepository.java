package com.project.elms.repository;
import com.project.elms.model.Course;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
 
@Repository
public interface CourseRepository extends JpaRepository<Course, Integer> {
 
	@Query(value = "SELECT c.* FROM course c " +
	               "INNER JOIN course_enrollment ce ON ce.course_id = c.course_id " +
	               "WHERE ce.student_id = :studentId", nativeQuery = true)
    List<Course> findByStudentId(@Param("studentId") Integer studentId);

    @Query(value = "SELECT * FROM course WHERE instructor_id = :instructorId", nativeQuery = true)
    List<Course> findByInstructorId(@Param("instructorId") Integer instructorId);
	
	boolean existsByTitle(String title);

	List<Course> findByTitleContainingIgnoreCase(String keyword);

    @Query(value = "SELECT * FROM course WHERE instructor_id = :instructorId AND assignment_status = :status", nativeQuery = true)
    List<Course> findByInstructorIdAndAssignmentStatus(@Param("instructorId") Integer instructorId, @Param("status") String status);

    List<Course> findByAssignmentStatus(Course.AssignmentStatus assignmentStatus);
}