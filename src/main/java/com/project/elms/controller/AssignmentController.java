package com.project.elms.controller;

import com.project.elms.model.Assignment;
import com.project.elms.model.Submission;
import com.project.elms.service.AssignmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    @Autowired
    private AssignmentService assignmentService;

    @PostMapping({"", "/create"})
    public ResponseEntity<Assignment> createAssignment(@RequestBody Map<String, Object> payload) {
        Assignment assignment = new Assignment();
        assignment.setTitle((String) payload.get("title"));
        assignment.setMaxScore(Integer.parseInt(payload.get("maxScore").toString()));
         if (payload.containsKey("dueDate")) {
            try {
                String dueDateStr = payload.get("dueDate").toString();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                Date parsedDate = dateFormat.parse(dueDateStr);
                assignment.setDueDate(parsedDate);
            } catch (Exception e) {
              
                System.err.println("Failed to parse dueDate: " + e.getMessage());
            }
        }
        
        if (payload.containsKey("description")) {
            assignment.setDescription((String) payload.get("description"));
        }
 
        Integer courseId = Integer.parseInt(payload.get("courseId").toString());
        
        Assignment savedAssignment = assignmentService.createAssignment(assignment, courseId);
        return ResponseEntity.ok(savedAssignment);
    }

    @PostMapping("/submit")
    public ResponseEntity<Submission> submitAssignment(@RequestBody Submission submission) {
        return ResponseEntity.ok(assignmentService.submitAssignment(submission));
    }

    
    @PostMapping("/submit-quiz")
    public ResponseEntity<Map<String, Object>> submitQuiz(@RequestBody Map<String, Object> payload) {
        Long assignmentId = Long.parseLong(payload.get("assignmentId").toString());
        Integer studentId = Integer.parseInt(payload.get("studentId").toString());
        Integer score = Integer.parseInt(payload.get("score").toString());
        String answer = payload.containsKey("answer") ? payload.get("answer").toString() : null;
        return ResponseEntity.ok(assignmentService.submitQuiz(assignmentId, studentId, score, answer));
    }


    @GetMapping("/check-submission/{assignmentId}/{studentId}")
    public ResponseEntity<Map<String, Object>> checkSubmission(
            @PathVariable Long assignmentId, @PathVariable Integer studentId) {
        boolean exists = assignmentService.hasSubmitted(assignmentId, studentId);
        return ResponseEntity.ok(Map.of("submitted", exists));
    }

    @PutMapping("/grade/{submissionId}")
    public ResponseEntity<Submission> gradeAssignment(
            @PathVariable Long submissionId,
            @RequestParam Integer score,
            @RequestParam String feedback) {
        return ResponseEntity.ok(assignmentService.gradeAssignment(submissionId, score, feedback));
    }

    @GetMapping("/retake-info/{assignmentId}/{studentId}")
    public ResponseEntity<Map<String, Object>> getRetakeInfo(
            @PathVariable Long assignmentId, @PathVariable Integer studentId) {
        return ResponseEntity.ok(assignmentService.getRetakeInfo(assignmentId, studentId));
    }
    @GetMapping("/course/{courseId}/certificate/{studentId}")
    public ResponseEntity<Map<String, Object>> getCourseCertificateStatus(
            @PathVariable Integer courseId, @PathVariable Integer studentId) {
        return ResponseEntity.ok(assignmentService.getCertificateStatus(courseId, studentId));
    }
   @GetMapping("/all")
    public ResponseEntity<List<Assignment>> getAllAssignments() {
        return ResponseEntity.ok(assignmentService.getAllAssignments());
    }

    @GetMapping("/{assignmentId}")
    public ResponseEntity<Assignment> getAssignmentById(@PathVariable Long assignmentId) {
        return ResponseEntity.ok(assignmentService.getAssignmentById(assignmentId));
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<Assignment>> getAssignmentsByCourse(@PathVariable Integer courseId) {
        return ResponseEntity.ok(assignmentService.getAssignmentsByCourse(courseId));
    }
  
    @GetMapping("/grades/{assignmentId}")
    public ResponseEntity<List<Submission>> getGrades(@PathVariable Long assignmentId) {
        return ResponseEntity.ok(assignmentService.getGradesByAssignment(assignmentId));
    }

    @PutMapping("/{assignmentId}/extend-due-date")
    public ResponseEntity<Map<String, Object>> extendDueDate(
            @PathVariable Long assignmentId,
            @RequestBody Map<String, Object> payload) {
        
        if (!payload.containsKey("newDueDate")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing required field: newDueDate"));
        }
        
        String newDueDateStr = payload.get("newDueDate").toString();
        try {
            
            java.time.LocalDate localDate = java.time.LocalDate.parse(newDueDateStr);
           
            java.util.Date newDueDate = java.util.Date.from(localDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
            
            return ResponseEntity.ok(assignmentService.extendDueDate(assignmentId, newDueDate));
        } catch (java.time.format.DateTimeParseException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid date format. Use yyyy-MM-dd."));
        }
    }
}