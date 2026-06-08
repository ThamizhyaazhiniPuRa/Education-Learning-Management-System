package com.project.elms.model;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "message")
public class Message {

    public enum MessageType { ATTENDANCE_QUERY, ASSIGNMENT_EXTENSION, GENERAL }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Integer messageId;

    /** The student who sent/received the thread */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /** Course the message is about */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /** Instructor this message is directed to */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id")
    private Student instructor;

    /** STUDENT or INSTRUCTOR (who sent THIS message line) */
    @Column(name = "sender_role", nullable = false)
    private String senderRole;   // "STUDENT" | "INSTRUCTOR"

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private MessageType messageType;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "sent_at", nullable = false)
    private Date sentAt;

    /** Groups back-and-forth messages into a thread (first messageId in thread) */
    @Column(name = "thread_id")
    private Integer threadId;

    /** Has the recipient read this message? */
    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    // ── Getters / Setters ──

    public Integer getMessageId() { return messageId; }
    public void setMessageId(Integer messageId) { this.messageId = messageId; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public Student getInstructor() { return instructor; }
    public void setInstructor(Student instructor) { this.instructor = instructor; }

    public String getSenderRole() { return senderRole; }
    public void setSenderRole(String senderRole) { this.senderRole = senderRole; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public MessageType getMessageType() { return messageType; }
    public void setMessageType(MessageType messageType) { this.messageType = messageType; }

    public Date getSentAt() { return sentAt; }
    public void setSentAt(Date sentAt) { this.sentAt = sentAt; }

    public Integer getThreadId() { return threadId; }
    public void setThreadId(Integer threadId) { this.threadId = threadId; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
}
