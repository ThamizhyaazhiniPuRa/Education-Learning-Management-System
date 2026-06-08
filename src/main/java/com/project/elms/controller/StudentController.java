package com.project.elms.controller;
 
import com.project.elms.dto.SignupRequest;
import com.project.elms.dto.StudentSummary;
import com.project.elms.model.*;
import com.project.elms.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;
 

 
@RestController
@RequestMapping("/api/students")
public class StudentController {
 
    @Autowired
    private StudentService studentService;
 
    @Autowired
    private CourseService courseService;
 
    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private AuthService authService;

    @Autowired
    private AnalyticsService analyticsService;
 
    // IMPORTANT: Specific routes MUST come before generic /{id} route
    // Otherwise /all will be interpreted as an ID parameter
    
    @GetMapping("/all")
    public ResponseEntity<List<StudentSummary>> getAllStudents() {
        // Refresh status for all students before returning
        List<Student> allStudents = studentService.getAllStudents();
        for (Student s : allStudents) {
            try {
                studentService.refreshStudentStatus(s.getStudentId());
            } catch (Exception ignored) {}
        }
        return ResponseEntity.ok(studentService.getAllStudentSummaries());
    }

//    @PostMapping("/register")
//    public ResponseEntity<String> registerStudent(@RequestBody SignupRequest student) {
//        authService.registerStudent(student);
//        return ResponseEntity.ok("Registered");
//    }

    @PostMapping("/register")
    public ResponseEntity<String> registerStudent(@RequestBody SignupRequest request) {
        // Corrected: Route to authService, not studentService
        authService.registerStudent(request);
        return ResponseEntity.ok("Registered successfully");
    }
    @PutMapping("/{id}/complete")
    public ResponseEntity<String> markAsGraduated(@PathVariable Integer id) {
        studentService.markAsGraduated(id);
        return ResponseEntity.ok("Student with ID " + id + " has GRADUATED.");
    }

    @PutMapping("/{id}/drop")
    public ResponseEntity<String> markAsDropped(@PathVariable Integer id) {
        studentService.markAsDropped(id);
        return ResponseEntity.ok("Student with ID " + id + " has been marked as DROPPED.");
    }

    /**
     * Re-evaluates and persists the correct ACTIVE / GRADUATED / DROPPED status for a student.
     * Call this after any assignment submission or due-date check.
     */
    @PutMapping("/{id}/refresh-status")
    public ResponseEntity<Map<String, Object>> refreshStatus(@PathVariable Integer id) {
        Student.StudentStatus newStatus = studentService.refreshStudentStatus(id);
        return ResponseEntity.ok(Map.of("studentId", id, "status", newStatus != null ? newStatus.name() : "UNKNOWN"));
    }

    /**
     * Batch re-evaluate status for ALL students (admin cron / manual trigger).
     */
    @PutMapping("/refresh-all-statuses")
    public ResponseEntity<Map<String, Object>> refreshAllStatuses() {
        List<Student> all = studentService.getAllStudents();
        int updated = 0;
        for (Student s : all) {
            studentService.refreshStudentStatus(s.getStudentId());
            updated++;
        }
        return ResponseEntity.ok(Map.of("refreshed", updated));
    }

    @PutMapping("/profile/{id}")
    public ResponseEntity<Student> updateProfile(@PathVariable Integer id, @RequestBody Student student) {
        return ResponseEntity.ok(studentService.updateProfile(id, student));
    }
   
    @PostMapping("/{studentId}/enroll/{courseId}")
    public ResponseEntity<String> enrollCourse(@PathVariable Integer studentId, @PathVariable Integer courseId) {
        studentService.enrollCourse(studentId, courseId);
        return ResponseEntity.ok("Enrollment successful. Student is ACTIVE and Course is now ACTIVE.");
    }

    @GetMapping("/courses/all")
    public ResponseEntity<List<Course>> getAllCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    @GetMapping("/courses/{courseId}")
    public ResponseEntity<Course> getCourseById(@PathVariable Integer courseId) {
        return courseService.getCourseDetails(courseId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/courses/search")
    public ResponseEntity<List<Course>> searchCourses(@RequestParam String keyword) {
        return ResponseEntity.ok(courseService.searchCourses(keyword));
    }

    @GetMapping("/{studentId}/courses")
    public ResponseEntity<List<Course>> getStudentCourses(@PathVariable Integer studentId) {
        return ResponseEntity.ok(courseService.getCoursesByStudentId(studentId));
    }

    @GetMapping("/courses/{courseId}/assignments")
    public ResponseEntity<List<Assignment>> getAssignmentsByCourse(@PathVariable Integer courseId) {
        return ResponseEntity.ok(assignmentService.getAssignmentsByCourse(courseId));
    }

    @GetMapping("/{studentId}/submissions")
    public ResponseEntity<List<Submission>> getStudentSubmissions(@PathVariable Integer studentId) {
        return ResponseEntity.ok(assignmentService.getSubmissionsByStudent(studentId));
    }

    /**
     * Leaderboard: returns top students ranked by average quiz score.
     */
    @GetMapping("/leaderboard")
    public ResponseEntity<List<Map<String, Object>>> getLeaderboard() {
        List<Student> allStudents = studentService.getAllStudents();
        List<Map<String, Object>> board = new ArrayList<>();

        for (Student s : allStudents) {
            List<Submission> subs = assignmentService.getSubmissionsByStudent(s.getStudentId());
            if (subs.isEmpty()) continue;
            double avg = subs.stream()
                    .filter(sub -> sub.getScore() != null)
                    .mapToInt(Submission::getScore)
                    .average().orElse(0);
            long count = subs.stream().filter(sub -> sub.getScore() != null).count();
            if (count == 0) continue;

            Map<String, Object> learning = analyticsService.getStudentLearningInsights(s.getStudentId());
            int xpPoints = ((Number) learning.getOrDefault("xpPoints", 0)).intValue();
            String xpStage = String.valueOf(learning.getOrDefault("xpStage", "Beginner"));

            @SuppressWarnings("unchecked")
            Map<String, Object> rubric = (Map<String, Object>) learning.getOrDefault("rubric", Collections.emptyMap());
            String rubricStatus = resolveRubricStatus(rubric);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> continueLearning = (List<Map<String, Object>>) learning.getOrDefault("continueLearning", Collections.emptyList());
            int overallCompletion = continueLearning.isEmpty()
                    ? 0
                    : (int) Math.round(continueLearning.stream()
                            .mapToInt(row -> ((Number) row.getOrDefault("progress", 0)).intValue())
                            .average()
                            .orElse(0));

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("studentId", s.getStudentId());
            entry.put("name", s.getName());
            entry.put("avgScore", Math.round(avg));
            entry.put("submissions", count);
            entry.put("xpPoints", xpPoints);
            entry.put("xpStage", xpStage);
            entry.put("rubricStatus", rubricStatus);
            entry.put("completionPercentage", overallCompletion);
            entry.put("status", rubricStatus);
            board.add(entry);
        }

        board.sort((a, b) -> {
            int byXp = Integer.compare(((Number) b.get("xpPoints")).intValue(), ((Number) a.get("xpPoints")).intValue());
            if (byXp != 0) return byXp;
            int byAvg = Integer.compare(((Number) b.get("avgScore")).intValue(), ((Number) a.get("avgScore")).intValue());
            if (byAvg != 0) return byAvg;
            return String.valueOf(a.get("name")).compareToIgnoreCase(String.valueOf(b.get("name")));
        });

        List<Map<String, Object>> top = board.stream().limit(10).collect(Collectors.toList());
        int previousRank = 0;
        int previousXp = Integer.MIN_VALUE;
        int position = 0;
        for (Map<String, Object> row : top) {
            position++;
            int xp = ((Number) row.getOrDefault("xpPoints", 0)).intValue();
            if (xp != previousXp) {
                previousRank = position;
                previousXp = xp;
            }
            row.put("rank", previousRank);
        }
        return ResponseEntity.ok(top);
    }

    private String resolveRubricStatus(Map<String, Object> rubric) {
        int exemplary = ((Number) rubric.getOrDefault("exemplary", 0)).intValue();
        int proficient = ((Number) rubric.getOrDefault("proficient", 0)).intValue();
        int developing = ((Number) rubric.getOrDefault("developing", 0)).intValue();
        int beginner = ((Number) rubric.getOrDefault("beginner", 0)).intValue();

        int max = Math.max(Math.max(exemplary, proficient), Math.max(developing, beginner));
        if (max == 0) return "Beginner";
        if (max == exemplary) return "Exemplary";
        if (max == proficient) return "Proficient";
        if (max == developing) return "Developing";
        return "Beginner";
    }

    // Generic route MUST come LAST - after all specific routes
    @GetMapping("/{id}")
    public ResponseEntity<Student> getStudentDetails(@PathVariable Integer id) {
        // Refresh status before returning student details
        try {
            studentService.refreshStudentStatus(id);
        } catch (Exception ignored) {}
        return studentService.getStudentDetails(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}