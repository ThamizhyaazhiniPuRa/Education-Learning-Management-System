package com.project.elms.controller;

import com.project.elms.model.*;
import com.project.elms.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API for the in-app course messaging feature.
 * Students send messages to their course instructor; instructors reply.
 *
 * Endpoints:
 *   POST   /api/messages/send                          – student sends first message (new thread)
 *   POST   /api/messages/reply                         – reply to an existing thread
 *   GET    /api/messages/student/{studentId}/course/{courseId}   – student's threads for a course
 *   GET    /api/messages/thread/{threadId}             – full conversation
 *   GET    /api/messages/instructor/{instructorId}     – all threads for instructor (inbox)
 *   GET    /api/messages/instructor/{instructorId}/course/{courseId} – instructor inbox scoped to course
 *   PUT    /api/messages/thread/{threadId}/read        – mark thread as read
 *   GET    /api/messages/unread/student/{studentId}    – unread count for student
 *   GET    /api/messages/unread/instructor/{instructorId} – unread count for instructor
 */
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired private MessageRepository  messageRepository;
    @Autowired private StudentRepository  studentRepository;
    @Autowired private CourseRepository   courseRepository;

    // ── Helper: map a Message to a plain DTO map ──────────────────────────
    private Map<String, Object> toMap(Message m) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("messageId",   m.getMessageId());
        dto.put("threadId",    m.getThreadId());
        dto.put("senderRole",  m.getSenderRole());
        dto.put("content",     m.getContent());
        dto.put("messageType", m.getMessageType() != null ? m.getMessageType().name() : "GENERAL");
        dto.put("sentAt",      m.getSentAt());
        dto.put("isRead",      m.isRead());
        dto.put("studentId",   m.getStudent() != null ? m.getStudent().getStudentId() : null);
        dto.put("studentName", m.getStudent() != null ? m.getStudent().getName() : null);
        dto.put("courseId",    m.getCourse()  != null ? m.getCourse().getCourseId()   : null);
        dto.put("courseTitle", m.getCourse()  != null ? m.getCourse().getTitle()      : null);
        dto.put("instructorId",   m.getInstructor() != null ? m.getInstructor().getStudentId() : null);
        dto.put("instructorName", m.getInstructor() != null ? m.getInstructor().getName()      : null);
        return dto;
    }

    // ── Helper: map thread root + latest reply count ──────────────────────
    private Map<String, Object> toThreadSummary(Message root) {
        Map<String, Object> dto = toMap(root);
        List<Message> thread = messageRepository.findByThreadIdOrderBySentAtAsc(root.getThreadId());
        dto.put("replyCount", thread.size() - 1);
        // last message preview
        if (thread.size() > 1) {
            Message last = thread.get(thread.size() - 1);
            dto.put("lastReply",     last.getContent());
            dto.put("lastReplySentAt", last.getSentAt());
            dto.put("lastReplySender", last.getSenderRole());
        }
        dto.put("hasUnreadReply", thread.stream()
            .anyMatch(msg -> !msg.isRead() && "INSTRUCTOR".equals(msg.getSenderRole())));
        return dto;
    }

    /**
     * POST /api/messages/send
     * Student initiates a new message thread.
     * Body: { studentId, courseId, content, messageType }
     */
    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, Object> body) {
        try {
            Integer studentId = Integer.parseInt(body.get("studentId").toString());
            Integer courseId  = Integer.parseInt(body.get("courseId").toString());
            String  content   = body.getOrDefault("content", "").toString().trim();
            String  typeStr   = body.getOrDefault("messageType", "GENERAL").toString().toUpperCase();

            if (content.isEmpty())
                return ResponseEntity.badRequest().body(Map.of("error", "Message content cannot be empty"));

            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new RuntimeException("Student not found"));
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new RuntimeException("Course not found"));

            // Resolve instructor for this course
            Student instructor = null;
            if (course.getInstructorId() != null) {
                instructor = studentRepository.findById(course.getInstructorId()).orElse(null);
            }

            Message.MessageType msgType;
            try { msgType = Message.MessageType.valueOf(typeStr); }
            catch (IllegalArgumentException e) { msgType = Message.MessageType.GENERAL; }

            Message msg = new Message();
            msg.setStudent(student);
            msg.setCourse(course);
            msg.setInstructor(instructor);
            msg.setSenderRole("STUDENT");
            msg.setContent(content);
            msg.setMessageType(msgType);
            msg.setSentAt(new Date());
            msg.setRead(false);

            // Save once to get the ID, then set threadId = messageId (this IS the root)
            Message saved = messageRepository.save(msg);
            saved.setThreadId(saved.getMessageId());
            saved = messageRepository.save(saved);

            return ResponseEntity.ok(toMap(saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/messages/reply
     * Either student or instructor replies to an existing thread.
     * Body: { threadId, senderRole, content, senderId }
     */
    @PostMapping("/reply")
    public ResponseEntity<?> reply(@RequestBody Map<String, Object> body) {
        try {
            Integer threadId   = Integer.parseInt(body.get("threadId").toString());
            String  senderRole = body.getOrDefault("senderRole", "STUDENT").toString().toUpperCase();
            String  content    = body.getOrDefault("content", "").toString().trim();

            if (content.isEmpty())
                return ResponseEntity.badRequest().body(Map.of("error", "Reply cannot be empty"));

            // Load the root message to copy metadata
            Message root = messageRepository.findById(threadId)
                    .orElseThrow(() -> new RuntimeException("Thread not found"));

            Message reply = new Message();
            reply.setStudent(root.getStudent());
            reply.setCourse(root.getCourse());
            reply.setInstructor(root.getInstructor());
            reply.setSenderRole(senderRole);
            reply.setContent(content);
            reply.setMessageType(root.getMessageType());
            reply.setSentAt(new Date());
            reply.setThreadId(threadId);
            reply.setRead(false);

            Message saved = messageRepository.save(reply);
            return ResponseEntity.ok(toMap(saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/messages/student/{studentId}/course/{courseId}
     * Returns thread roots for a student in one course.
     */
    @GetMapping("/student/{studentId}/course/{courseId}")
    public ResponseEntity<List<Map<String, Object>>> getStudentThreads(
            @PathVariable Integer studentId, @PathVariable Integer courseId) {
        List<Message> roots = messageRepository.findThreadsByStudentAndCourse(studentId, courseId);
        List<Map<String, Object>> result = roots.stream()
                .map(this::toThreadSummary).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/messages/thread/{threadId}
     * Returns all messages in a thread.
     */
    @GetMapping("/thread/{threadId}")
    public ResponseEntity<List<Map<String, Object>>> getThread(@PathVariable Integer threadId) {
        List<Message> msgs = messageRepository.findByThreadIdOrderBySentAtAsc(threadId);
        List<Map<String, Object>> result = msgs.stream().map(this::toMap).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/messages/instructor/{instructorId}
     * Returns all thread roots in an instructor's inbox.
     */
    @GetMapping("/instructor/{instructorId}")
    public ResponseEntity<List<Map<String, Object>>> getInstructorInbox(@PathVariable Integer instructorId) {
        List<Message> roots = messageRepository.findThreadsByInstructor(instructorId);
        List<Map<String, Object>> result = roots.stream()
                .map(this::toThreadSummary).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/messages/instructor/{instructorId}/course/{courseId}
     * Returns threads scoped to a course.
     */
    @GetMapping("/instructor/{instructorId}/course/{courseId}")
    public ResponseEntity<List<Map<String, Object>>> getInstructorInboxByCourse(
            @PathVariable Integer instructorId, @PathVariable Integer courseId) {
        List<Message> roots = messageRepository.findThreadsByInstructorAndCourse(instructorId, courseId);
        List<Map<String, Object>> result = roots.stream()
                .map(this::toThreadSummary).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * PUT /api/messages/thread/{threadId}/read
     * Mark all messages in a thread as read by the given role.
     * Body: { readerRole: "STUDENT" | "INSTRUCTOR" }
     */
    @PutMapping("/thread/{threadId}/read")
    public ResponseEntity<?> markRead(@PathVariable Integer threadId,
                                      @RequestBody Map<String, Object> body) {
        String readerRole = body.getOrDefault("readerRole", "INSTRUCTOR").toString().toUpperCase();
        // Mark messages sent by the opposite role as read (messages TO the reader)
        String senderToMark = readerRole.equals("INSTRUCTOR") ? "STUDENT" : "INSTRUCTOR";
        List<Message> thread = messageRepository.findByThreadIdOrderBySentAtAsc(threadId);
        thread.stream()
              .filter(m -> senderToMark.equals(m.getSenderRole()) && !m.isRead())
              .forEach(m -> { m.setRead(true); messageRepository.save(m); });
        return ResponseEntity.ok(Map.of("marked", "ok"));
    }

    /**
     * GET /api/messages/unread/student/{studentId}
     */
    @GetMapping("/unread/student/{studentId}")
    public ResponseEntity<Map<String, Object>> unreadForStudent(@PathVariable Integer studentId) {
        long count = messageRepository.countUnreadForStudent(studentId);
        return ResponseEntity.ok(Map.of("unread", count));
    }

    /**
     * GET /api/messages/unread/instructor/{instructorId}
     */
    @GetMapping("/unread/instructor/{instructorId}")
    public ResponseEntity<Map<String, Object>> unreadForInstructor(@PathVariable Integer instructorId) {
        long count = messageRepository.countUnreadForInstructor(instructorId);
        return ResponseEntity.ok(Map.of("unread", count));
    }
}
