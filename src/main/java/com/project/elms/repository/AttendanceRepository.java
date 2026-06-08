package com.project.elms.repository;
 
import com.project.elms.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Date;
import java.util.List;
import java.util.Optional;
 
@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Integer> {
  
    List<Attendance> findByStudent_StudentIdAndCourse_CourseId(Integer studentId, Integer courseId);
    List<Attendance> findByCourse_CourseId(Integer courseId);
    List<Attendance> findByStudent_StudentId(Integer studentId);

    /**
     * Find any attendance record for this student+course within today's window,
     * regardless of status (PRESENT or ABSENT). Used to detect duplicates.
     */
    @Query("SELECT a FROM Attendance a WHERE a.student.studentId = :studentId " +
           "AND a.course.courseId = :courseId " +
           "AND a.status = com.project.elms.model.Attendance.AttendanceStatus.PRESENT " +
           "AND a.date >= :startOfDay AND a.date < :startOfNextDay")
    Optional<Attendance> findTodayPresent(
            @Param("studentId")    Integer studentId,
            @Param("courseId")     Integer courseId,
            @Param("startOfDay")   Date startOfDay,
            @Param("startOfNextDay") Date startOfNextDay);

    /**
     * Count distinct students who have any attendance record today for a given course.
     */
    @Query("SELECT COUNT(DISTINCT a.student.studentId) FROM Attendance a " +
           "WHERE a.course.courseId = :courseId " +
           "AND a.date >= :startOfDay AND a.date < :startOfNextDay")
    long countMarkedTodayByCourse(
            @Param("courseId")       Integer courseId,
            @Param("startOfDay")     Date startOfDay,
            @Param("startOfNextDay") Date startOfNextDay);
}