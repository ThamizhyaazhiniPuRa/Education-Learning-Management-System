package com.project.elms.controller;

import com.project.elms.model.Attendance;
import com.project.elms.model.Course;
import com.project.elms.model.Role;
import com.project.elms.model.Student;
import com.project.elms.repository.AttendanceRepository;
import com.project.elms.repository.CourseRepository;
import com.project.elms.repository.StudentRepository;
import com.project.elms.service.AttendanceService;
import com.project.elms.service.CourseService;
import com.project.elms.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API endpoints used by the Instructor Dashboard.
 * Instructors can only see courses they are assigned to.
 */
@RestController
@RequestMapping("/api/instructor")
public class InstructorController {

    @Autowired private CourseService courseService;
    @Autowired private StudentService studentService;
    @Autowired private AttendanceService attendanceService;
    @Autowired private CourseRepository courseRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private AttendanceRepository attendanceRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    /**
     * Returns the list of courses assigned to this instructor.
     * GET /api/instructor/{instructorId}/courses
     */
    @GetMapping("/{instructorId}/courses")
    public ResponseEntity<List<Course>> getAssignedCourses(@PathVariable Integer instructorId) {
        return ResponseEntity.ok(courseService.getCoursesByInstructorId(instructorId));
    }

    /**
     * Returns all students enrolled in a specific course.
     * GET /api/instructor/course/{courseId}/students
     * Uses a direct DB join: course.student_id → student.student_id
     */
    @GetMapping("/course/{courseId}/students")
    public ResponseEntity<List<Map<String, Object>>> getStudentsInCourse(@PathVariable Integer courseId) {
        // Use direct DB query instead of in-memory filter (fixes missing students issue)
        List<Student> enrolled = studentRepository.findByCourseId(courseId);
        
        // Refresh status for all enrolled students before returning
        for (Student s : enrolled) {
            try {
                studentService.refreshStudentStatus(s.getStudentId());
            } catch (Exception ignored) {}
        }
        // Re-fetch after status update
        enrolled = studentRepository.findByCourseId(courseId);

        List<Map<String, Object>> result = enrolled.stream()
            .map(s -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("studentId", s.getStudentId());
                m.put("name",      s.getName());
                m.put("email",     s.getEmail());
                m.put("program",   s.getProgram());
                m.put("status",    s.getStatus() != null ? s.getStatus().name() : "ACTIVE");
                return m;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * Returns attendance summary for a student in a course.
     * Also includes whether today is already marked and what status.
     * GET /api/instructor/attendance/{studentId}/{courseId}
     */
    @GetMapping("/attendance/{studentId}/{courseId}")
    public ResponseEntity<Map<String, Object>> getAttendanceSummary(
            @PathVariable Integer studentId, @PathVariable Integer courseId) {

        List<Attendance> records = attendanceRepository
                .findByStudent_StudentIdAndCourse_CourseId(studentId, courseId);

        long present = records.stream()
                .filter(a -> a.getStatus() == Attendance.AttendanceStatus.PRESENT)
                .count();
        int total = records.size();
        int pct   = total > 0 ? (int) Math.round((double) present / total * 100) : 0;

        // Check if already marked TODAY
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);      cal.set(Calendar.MILLISECOND, 0);
        Date startOfDay = cal.getTime();
        cal.add(Calendar.DATE, 1);
        Date startOfNextDay = cal.getTime();

        java.util.Optional<Attendance> todayRecord = records.stream()
                .filter(a -> a.getDate() != null
                          && !a.getDate().before(startOfDay)
                          && a.getDate().before(startOfNextDay))
                .findFirst();

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("total",      total);
        res.put("present",    (int) present);
        res.put("absent",     total - (int) present);
        res.put("percentage", pct);
        res.put("markedToday",  todayRecord.isPresent());
        res.put("todayStatus",  todayRecord.map(a -> a.getStatus().name()).orElse(null));
        return ResponseEntity.ok(res);
    }

    /**
     * Returns aggregated stats for the instructor dashboard stat cards.
     * GET /api/instructor/{instructorId}/stats
     * Returns: { totalCourses, totalStudents, markedToday, avgAttendancePct }
     */
    @GetMapping("/{instructorId}/stats")
    public ResponseEntity<Map<String, Object>> getInstructorStats(@PathVariable Integer instructorId) {
        List<Course> courses = courseService.getCoursesByInstructorId(instructorId).stream()
            .filter(c -> c.getAssignmentStatus() == null ||
                         c.getAssignmentStatus() == com.project.elms.model.Course.AssignmentStatus.ACCEPTED)
            .collect(Collectors.toList());

        // Refresh status for all students in instructor's courses
        for (Course c : courses) {
            try {
                studentService.refreshAllStudentsInCourse(c.getCourseId());
            } catch (Exception ignored) {}
        }

        // Today's window
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);      cal.set(Calendar.MILLISECOND, 0);
        Date startOfDay = cal.getTime();
        cal.add(Calendar.DATE, 1);
        Date startOfNextDay = cal.getTime();

        int totalStudents  = 0;
        long markedToday   = 0;
        double totalPctSum = 0;
        int courseCount    = courses.size();

        for (Course c : courses) {
            List<Student> enrolled = studentRepository.findByCourseId(c.getCourseId());
            totalStudents += enrolled.size();

            // Count how many of those students are marked today
            long markedInCourse = attendanceRepository
                .countMarkedTodayByCourse(c.getCourseId(), startOfDay, startOfNextDay);
            markedToday += markedInCourse;

            // Avg attendance % for this course
            List<Attendance> allAtt = attendanceRepository.findByCourse_CourseId(c.getCourseId());
            if (!enrolled.isEmpty() && !allAtt.isEmpty()) {
                long presentCount = allAtt.stream()
                    .filter(a -> a.getStatus() == Attendance.AttendanceStatus.PRESENT).count();
                totalPctSum += (double) presentCount / allAtt.size() * 100;
            }
        }

        int avgPct = courseCount > 0 ? (int) Math.round(totalPctSum / courseCount) : 0;

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalCourses",      courseCount);
        stats.put("totalStudents",     totalStudents);
        stats.put("markedToday",       markedToday);
        stats.put("avgAttendancePct",  avgPct);
        return ResponseEntity.ok(stats);
    }

    /**
     * Manually mark attendance for a student in a course (PRESENT or ABSENT).
     * POST /api/instructor/attendance/mark
     * Body: { "studentId": 1, "courseId": 2, "status": "PRESENT" }
     */
    @PostMapping("/attendance/mark")
    public ResponseEntity<?> markAttendance(@RequestBody Map<String, Object> payload) {
        try {
            Integer studentId  = Integer.parseInt(payload.get("studentId").toString());
            Integer courseId   = Integer.parseInt(payload.get("courseId").toString());
            String  statusStr  = payload.getOrDefault("status", "PRESENT").toString().toUpperCase();
            Attendance.AttendanceStatus status = Attendance.AttendanceStatus.valueOf(statusStr);

            // Check if already marked today for this course
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);      cal.set(Calendar.MILLISECOND, 0);
            Date startOfDay = cal.getTime();
            cal.add(Calendar.DATE, 1);
            Date startOfNextDay = cal.getTime();

            Optional<Attendance> existing = attendanceRepository
                    .findTodayPresent(studentId, courseId, startOfDay, startOfNextDay);

            if (existing.isPresent()) {
                // Update existing record's status
                Attendance att = existing.get();
                att.setStatus(status);
                att.setDate(new Date());
                attendanceRepository.save(att);
                return ResponseEntity.ok(Map.of("message", "Attendance updated", "status", status));
            }

            // Also check for any today record regardless of status
            List<Attendance> todayAll = attendanceRepository
                    .findByStudent_StudentIdAndCourse_CourseId(studentId, courseId)
                    .stream()
                    .filter(a -> a.getDate() != null && !a.getDate().before(startOfDay) && a.getDate().before(startOfNextDay))
                    .collect(Collectors.toList());

            if (!todayAll.isEmpty()) {
                Attendance att = todayAll.get(0);
                att.setStatus(status);
                att.setDate(new Date());
                attendanceRepository.save(att);
                return ResponseEntity.ok(Map.of("message", "Attendance updated", "status", status));
            }

            // Create new
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new RuntimeException("Student not found"));
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new RuntimeException("Course not found"));

            Attendance att = new Attendance();
            att.setStudent(student);
            att.setCourse(course);
            att.setDate(new Date());
            att.setStatus(status);
            attendanceRepository.save(att);

            return ResponseEntity.ok(Map.of("message", "Attendance marked", "status", status));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Returns all instructors (students with role INSTRUCTOR).
     * GET /api/instructor/all
     */
    @GetMapping("/all")
    public ResponseEntity<List<Map<String, Object>>> getAllInstructors() {
        List<Map<String, Object>> instructors = studentRepository.findAll().stream()
            .filter(s -> s.getRole() != null && s.getRole().name().equals("INSTRUCTOR"))
            .map(s -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("instructorId", s.getStudentId());
                m.put("name",         s.getName());
                m.put("email",        s.getEmail());
                m.put("program",      s.getProgram());
                return m;
            })
            .collect(Collectors.toList());
        return ResponseEntity.ok(instructors);
    }

    /**
     * Assign an instructor to a course.
     * POST /api/instructor/assign-course
     * Body: { "courseId": 1, "instructorId": 5 }
     */
    @PostMapping("/assign-course")
    public ResponseEntity<?> assignInstructorToCourse(@RequestBody Map<String, Object> payload) {
        try {
            Integer courseId     = Integer.parseInt(payload.get("courseId").toString());
            Integer instructorId = Integer.parseInt(payload.get("instructorId").toString());

            // Verify the instructor exists and has INSTRUCTOR role
            Student instructor = studentRepository.findById(instructorId)
                    .orElseThrow(() -> new RuntimeException("Instructor not found with id: " + instructorId));
            if (instructor.getRole() != Role.INSTRUCTOR) {
                return ResponseEntity.badRequest().body(Map.of("error", "Selected user is not an Instructor"));
            }

            courseService.assignInstructor(courseId, instructorId);
            return ResponseEntity.ok(Map.of(
                "message", "Instructor '" + instructor.getName() + "' assigned to course #" + courseId + " (Pending acceptance)"
            ));
        } catch (IllegalArgumentException e) {
            // 🌟 Captures the custom verification exception message and passes it gracefully back to the view component
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Instructor accepts or rejects an assigned course.
     * POST /api/instructor/respond-assignment
     * Body: { "courseId": 1, "instructorId": 5, "accepted": true }
     */
    @PostMapping("/respond-assignment")
    public ResponseEntity<?> respondToAssignment(@RequestBody Map<String, Object> payload) {
        try {
            Integer courseId     = Integer.parseInt(payload.get("courseId").toString());
            Integer instructorId = Integer.parseInt(payload.get("instructorId").toString());
            boolean accepted     = Boolean.parseBoolean(payload.get("accepted").toString());

            courseService.respondToAssignment(courseId, instructorId, accepted);
            return ResponseEntity.ok(Map.of(
                "message", "Course assignment " + (accepted ? "accepted" : "rejected") + " successfully"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get pending course assignments for an instructor.
     * GET /api/instructor/{instructorId}/pending-assignments
     */
    @GetMapping("/{instructorId}/pending-assignments")
    public ResponseEntity<List<Map<String, Object>>> getPendingAssignments(@PathVariable Integer instructorId) {
        List<Map<String, Object>> result = courseService.getCoursesByInstructorId(instructorId).stream()
            .filter(c -> c.getAssignmentStatus() != null)
            .map(c -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("courseId",         c.getCourseId());
                m.put("title",            c.getTitle());
                m.put("description",      c.getDescription());
                m.put("semester",         c.getSemester());
                m.put("credits",          c.getCredits());
                m.put("status",           c.getStatus());
                m.put("assignmentStatus", c.getAssignmentStatus());
                return m;
            })
            .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * Admin: view all course assignments with their status.
     * GET /api/instructor/assignments/all
     */
    @GetMapping("/assignments/all")
    public ResponseEntity<List<Map<String, Object>>> getAllAssignments() {
        List<Map<String, Object>> result = courseService.getAllCoursesWithAssignment().stream()
            .map(c -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("courseId",         c.getCourseId());
                m.put("title",            c.getTitle());
                m.put("semester",         c.getSemester());
                m.put("instructorId",     c.getInstructorId());
                m.put("assignmentStatus", c.getAssignmentStatus());
                // Fetch instructor name
                if (c.getInstructorId() != null) {
                    studentRepository.findById(c.getInstructorId()).ifPresent(s ->
                        m.put("instructorName", s.getName())
                    );
                }
                return m;
            })
            .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * Register a new instructor (admin-only action done from admin UI).
     * POST /api/instructor/register
     * Body: { "name": "...", "email": "...", "phone": "...", "password": "...", "program": "..." }
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerInstructor(@RequestBody Map<String, Object> payload) {
        try {
            // --- Null-safe field extraction ---
            Object nameObj  = payload.get("name");
            Object emailObj = payload.get("email");
            Object phoneObj = payload.get("phone");
            Object passObj  = payload.get("password");

            if (nameObj == null || emailObj == null || phoneObj == null || passObj == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "name, email, phone and password are required"));
            }

            String name     = nameObj.toString().trim();
            String email    = emailObj.toString().trim().toLowerCase();
            String phone    = phoneObj.toString().trim().replaceAll("[^0-9]", ""); // digits only
            String password = passObj.toString();
            String program  = (payload.containsKey("program") && payload.get("program") != null)
                              ? payload.get("program").toString().trim() : "Instructor";

            if (name.isEmpty())     return ResponseEntity.badRequest().body(Map.of("error", "Name cannot be empty"));
            if (email.isEmpty())    return ResponseEntity.badRequest().body(Map.of("error", "Email cannot be empty"));
            if (phone.length() != 10) return ResponseEntity.badRequest().body(Map.of("error", "Phone must be exactly 10 digits"));
            if (password.length() < 6) return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 6 characters"));
            if (program.isEmpty())  program = "Instructor";

            if (studentRepository.existsByEmail(email)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email already registered: " + email));
            }
            if (studentRepository.existsByPhone(phone)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Phone number already exists: " + phone));
            }

            Student instructor = new Student();
            instructor.setName(name);
            instructor.setEmail(email);
            instructor.setPhone(phone);
            instructor.setProgram(program);
            instructor.setPasswordHash(passwordEncoder.encode(password));
            instructor.setRole(Role.INSTRUCTOR);
            instructor.setEnabled(true);
            instructor.setStatus(Student.StudentStatus.ACTIVE);

            Student saved = studentRepository.save(instructor);
            return ResponseEntity.ok(Map.of(
                "message",      "Instructor registered successfully",
                "instructorId", saved.getStudentId(),
                "name",         saved.getName(),
                "email",        saved.getEmail()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to create instructor: " + e.getMessage()));
        }
    }
}
