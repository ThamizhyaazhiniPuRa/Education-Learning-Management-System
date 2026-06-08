package com.project.elms.controller;
 
import com.project.elms.model.Attendance;
import com.project.elms.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
 
@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {
 
    @Autowired
    private AttendanceService attendanceService;
 
    @PostMapping("/mark")
    public ResponseEntity<Attendance> markAttendance(@RequestBody Attendance attendance) {
        return ResponseEntity.ok(attendanceService.markAttendance(attendance));
    }
 
    @GetMapping("/summary/{studentId}/{courseId}")
    public ResponseEntity<?> getAttendanceSummary(@PathVariable Integer studentId, @PathVariable Integer courseId) {
        return ResponseEntity.ok(attendanceService.getSummary(studentId, courseId));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> getStudentAttendance(@PathVariable Integer studentId) {
        return ResponseEntity.ok(attendanceService.getByStudent(studentId));
    }
 
    @PostMapping("/participation")
    public ResponseEntity<String> recordParticipation(@RequestParam Integer studentId, @RequestParam Integer courseId) {
        attendanceService.recordParticipation(studentId, courseId);
        return ResponseEntity.ok("Participation recorded.");
    }
}