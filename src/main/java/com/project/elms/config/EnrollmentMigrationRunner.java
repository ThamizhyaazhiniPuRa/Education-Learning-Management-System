package com.project.elms.config;

import com.project.elms.model.CourseEnrollment;
import com.project.elms.repository.CourseEnrollmentRepository;
import com.project.elms.repository.CourseRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Runs once on startup to migrate old course.student_id enrollments
 * into the new course_enrollment join table.
 * Safe to run multiple times — skips duplicates.
 */
@Component
public class EnrollmentMigrationRunner implements ApplicationRunner {

    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;

    public EnrollmentMigrationRunner(CourseRepository courseRepository,
                                     CourseEnrollmentRepository courseEnrollmentRepository) {
        this.courseRepository = courseRepository;
        this.courseEnrollmentRepository = courseEnrollmentRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        courseRepository.findAll().forEach(course -> {
            Integer studentId = course.getStudentId();
            Integer courseId  = course.getCourseId();
            if (studentId != null && courseId != null) {
                if (!courseEnrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
                    courseEnrollmentRepository.save(new CourseEnrollment(studentId, courseId));
                    System.out.println("[Migration] Enrolled student " + studentId + " → course " + courseId);
                }
            }
        });
    }
}
