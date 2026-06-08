package com.project.elms.repository;

import com.project.elms.model.CourseEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, Integer> {

    List<CourseEnrollment> findByCourseId(Integer courseId);

    List<CourseEnrollment> findByStudentId(Integer studentId);

    Optional<CourseEnrollment> findByStudentIdAndCourseId(Integer studentId, Integer courseId);

    boolean existsByStudentIdAndCourseId(Integer studentId, Integer courseId);
}
