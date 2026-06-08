package com.project.elms.repository;

import com.project.elms.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Integer> {

    /** All messages in a thread (ordered by time) */
    List<Message> findByThreadIdOrderBySentAtAsc(Integer threadId);

    /** All threads for a student in a course (one row per thread root) */
    @Query("SELECT m FROM Message m WHERE m.student.studentId = :studentId AND m.course.courseId = :courseId AND m.messageId = m.threadId ORDER BY m.sentAt DESC")
    List<Message> findThreadsByStudentAndCourse(@Param("studentId") Integer studentId, @Param("courseId") Integer courseId);

    /** All threads an instructor has received (across all courses) */
    @Query("SELECT m FROM Message m WHERE m.instructor.studentId = :instructorId AND m.messageId = m.threadId ORDER BY m.sentAt DESC")
    List<Message> findThreadsByInstructor(@Param("instructorId") Integer instructorId);

    /** All threads an instructor has received for a specific course */
    @Query("SELECT m FROM Message m WHERE m.instructor.studentId = :instructorId AND m.course.courseId = :courseId AND m.messageId = m.threadId ORDER BY m.sentAt DESC")
    List<Message> findThreadsByInstructorAndCourse(@Param("instructorId") Integer instructorId, @Param("courseId") Integer courseId);

    /** Count unread messages for instructor */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.instructor.studentId = :instructorId AND m.isRead = false AND m.senderRole = 'STUDENT'")
    long countUnreadForInstructor(@Param("instructorId") Integer instructorId);

    /** Count unread replies for student */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.student.studentId = :studentId AND m.isRead = false AND m.senderRole = 'INSTRUCTOR'")
    long countUnreadForStudent(@Param("studentId") Integer studentId);
}
