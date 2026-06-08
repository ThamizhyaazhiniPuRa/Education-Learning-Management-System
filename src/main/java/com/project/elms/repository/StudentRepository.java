package com.project.elms.repository;

import com.project.elms.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Integer> {
    Optional<Student> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    
    @Query("SELECT s FROM Student s WHERE s.studentId IS NOT NULL")
    List<Student> findAllWithUser();
    List<Student> findAll();

    /**
     * Find all students enrolled in a specific course via the course_enrollment join table.
     * Supports multiple students per course (many-to-many).
     */
    @Query(value = "SELECT s.* FROM student s " +
                   "INNER JOIN course_enrollment ce ON ce.student_id = s.student_id " +
                   "WHERE ce.course_id = :courseId AND s.role = 'STUDENT'",
           nativeQuery = true)
    List<Student> findByCourseId(@Param("courseId") Integer courseId);
}