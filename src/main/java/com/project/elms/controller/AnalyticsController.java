package com.project.elms.controller;

import com.project.elms.model.AnalyticsSnapshot;
import com.project.elms.model.AnalyticsReport;
import com.project.elms.model.Student;
import com.project.elms.service.AnalyticsService;
import com.project.elms.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @Autowired private AnalyticsService analyticsService;
    @Autowired private StudentService studentService;

    @GetMapping("/performance/{courseId}")
    public ResponseEntity<Map<String, Object>> getPerformanceReport(@PathVariable Integer courseId) {
        return ResponseEntity.ok(analyticsService.getPerformanceReport(courseId));
    }

    @PostMapping("/generate/{courseId}")
    public ResponseEntity<AnalyticsSnapshot> generateSnapshot(@PathVariable Integer courseId) {
        return ResponseEntity.ok(analyticsService.generateSnapshot(courseId));
    }

    @GetMapping("/export/{courseId}")
    public ResponseEntity<String> exportAnalytics(@PathVariable Integer courseId) {
        return ResponseEntity.ok(analyticsService.exportCSV(courseId));
    }

    @GetMapping("/attendance/overall")
    public ResponseEntity<Map<String, Object>> getOverallAttendance() {
        return ResponseEntity.ok(analyticsService.getOverallAttendance());
    }

    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getAdminOverview() {
        // Refresh all student statuses before returning overview
        try {
            List<Student> students = studentService.getAllStudents();
            for (Student s : students) {
                try {
                    studentService.refreshStudentStatus(s.getStudentId());
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return ResponseEntity.ok(analyticsService.getAdminAnalyticsOverview());
    }

//    @GetMapping("/attendance/student/{studentId}")
//    public ResponseEntity<Map<String, Object>> getStudentAttendance(@PathVariable Integer studentId) {
//        return ResponseEntity.ok(analyticsService.getStudentAttendance(studentId));
//    }
 // CHANGE THIS LINE to match your frontend URL structure:
    
 // Handler 1: Processes general dashboard queries (No Course Context)
    @GetMapping("/attendance/student/{studentId}")
    public ResponseEntity<Map<String, Object>> getStudentAttendanceGlobal(
            @PathVariable Integer studentId) {
        return ResponseEntity.ok(analyticsService.getStudentAttendance(studentId));
    }

    // Handler 2: Processes targeted module selections (With Course Context)
    @GetMapping("/attendance/course/{courseId}/student/{studentId}")
    public ResponseEntity<Map<String, Object>> getStudentAttendanceByCourse(
            @PathVariable Integer courseId, 
            @PathVariable Integer studentId) {
        // Both cleanly pass the required student identifier down to the calculation engine
        return ResponseEntity.ok(analyticsService.getStudentAttendance(studentId));
    }
    @GetMapping("/gpa/student/{studentId}")
    public ResponseEntity<Map<String, Object>> getStudentGPA(@PathVariable Integer studentId) {
        // Refresh student status before returning GPA
        try {
            studentService.refreshStudentStatus(studentId);
        } catch (Exception ignored) {}
        return ResponseEntity.ok(analyticsService.calculateStudentGPA(studentId));
    }
    

    @GetMapping("/export/grades/{courseId}")
    public ResponseEntity<byte[]> exportGradesCSV(@PathVariable Integer courseId) {
        String csv = analyticsService.exportGradesCSV(courseId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=grades_course_" + courseId + ".csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.getBytes());
    }

    @GetMapping("/progress/students")
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> getStudentProgress() {
        // Refresh all student statuses before returning progress
        try {
            List<Student> students = studentService.getAllStudents();
            for (Student s : students) {
                try {
                    studentService.refreshStudentStatus(s.getStudentId());
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return ResponseEntity.ok(analyticsService.getStudentProgressSummaries());
    }
    
    @PostMapping("/export/save")
    public ResponseEntity<AnalyticsReport> saveExportedReport(@RequestBody Map<String, Object> payload) {
        String reportType = (String) payload.getOrDefault("reportType", "OVERVIEW");
        String csvData = (String) payload.getOrDefault("csvData", "");
        String generatedBy = (String) payload.getOrDefault("generatedBy", "Admin");
        
        Map<String, Object> overview = analyticsService.getAdminAnalyticsOverview();
        AnalyticsReport saved = analyticsService.saveAnalyticsExport(reportType, csvData, generatedBy, overview);
        return ResponseEntity.ok(saved);
    }
    
    @GetMapping("/reports/recent")
    public ResponseEntity<List<AnalyticsReport>> getRecentReports() {
        return ResponseEntity.ok(analyticsService.getRecentReports());
    }
    
    @PostMapping("/snapshots/generate-all")
    public ResponseEntity<Map<String, Object>> generateAllSnapshots() {
        return ResponseEntity.ok(analyticsService.generateAllSnapshots());
    }
    
    @GetMapping("/snapshots/all")
    public ResponseEntity<List<AnalyticsSnapshot>> getAllSnapshots() {
        return ResponseEntity.ok(analyticsService.getAllSnapshots());
    }
    
    @GetMapping("/trends/{courseId}")
    public ResponseEntity<Map<String, Object>> getCourseTrends(@PathVariable Integer courseId) {
        return ResponseEntity.ok(analyticsService.getCourseTrends(courseId));
    }
    
    @GetMapping("/comparative")
    public ResponseEntity<Map<String, Object>> getComparativeAnalytics() {
        return ResponseEntity.ok(analyticsService.getComparativeAnalytics());
    }

    @GetMapping("/student/{studentId}/learning")
    public ResponseEntity<Map<String, Object>> getStudentLearningInsights(@PathVariable Integer studentId) {
        // Refresh student status before returning learning insights
        try {
            studentService.refreshStudentStatus(studentId);
        } catch (Exception ignored) {}
        return ResponseEntity.ok(analyticsService.getStudentLearningInsights(studentId));
    }

    @GetMapping("/reports/learning")
    public ResponseEntity<List<Map<String, Object>>> getLearningReport() {
        // Refresh all student statuses before returning learning report
        try {
            List<Student> students = studentService.getAllStudents();
            for (Student s : students) {
                try {
                    studentService.refreshStudentStatus(s.getStudentId());
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return ResponseEntity.ok(analyticsService.getAllStudentsLearningInsights());
    }
}