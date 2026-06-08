package com.project.elms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Autowired;
import com.project.elms.service.CourseService;
import com.project.elms.service.StudentService;
import com.project.elms.service.AssignmentService;
import com.project.elms.service.AnalyticsService;
import com.project.elms.repository.SubmissionRepository;
import com.project.elms.model.Assignment;
import com.project.elms.model.Course;
import com.project.elms.model.Submission;
import java.util.stream.Collectors;
import java.util.Collections;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class WebController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private SubmissionRepository submissionRepository;

    /* ══════════════════════════════════════
       PUBLIC PAGES  (no auth needed)
     ══════════════════════════════════════ */

    @GetMapping({"/", "/home"})
    public String home() {
        return "Login/home";
    }

    @GetMapping("/register")
    public String register() {
        return "Login/register";
    }

    @GetMapping("/login")
    public String login() {
        return "Login/login";
    }

    /* ══════════════════════════════════════
       ADMIN PAGES
     ══════════════════════════════════════ */

    /** Dashboard — needs courses + students for stat cards & overview table */
    @GetMapping("/web/admin/dashboard")
    public String adminDashboard(Model model) {
        try {
            var courses = courseService.getAllCourses();
            var students = studentService.getAllStudents();
            
            // Refresh status for all students before displaying dashboard
            for (var student : students) {
                try {
                    studentService.refreshStudentStatus(student.getStudentId());
                } catch (Exception ignored) {}
            }
            var assignments = assignmentService.getAllAssignments();
            var submissions = submissionRepository.findAll();

            model.addAttribute("courses",  courses != null ? courses : Collections.emptyList());
            model.addAttribute("students", students != null ? students : Collections.emptyList());

            int totalCourses = courses != null ? courses.size() : 0;
            int activeCourses = courses != null ? (int) courses.stream().filter(c -> c.getStatus() == Course.CourseStatus.ACTIVE).count() : 0;
            int totalStudents = students != null ? students.size() : 0;
            int totalAssignments = assignments != null ? assignments.size() : 0;
            int pendingAssignments = assignments != null ? (int) assignments.stream().filter(a -> a.getDueDate() != null && a.getDueDate().after(new Date())).count() : 0;

            int attendancePercentage = (int) analyticsService.getOverallAttendance().getOrDefault("percentage", 0);

            List<Assignment> recentAssignments = assignments != null ? assignments.stream()
                    .sorted((a, b) -> {
                        if (a.getDueDate() == null) return 1;
                        if (b.getDueDate() == null) return -1;
                        return b.getDueDate().compareTo(a.getDueDate());
                    })
                    .limit(5)
                    .collect(Collectors.toList()) : Collections.emptyList();

            List<Map<String, Object>> courseEngagement = new ArrayList<>();
            if (courses != null) {
                for (Course course : courses.stream().limit(4).collect(Collectors.toList())) {
                    Map<String, Object> ce = new HashMap<>();
                    ce.put("title", course.getTitle());
                    ce.put("engagement", analyticsService.getCourseEngagementPercentage(course.getCourseId()));
                    courseEngagement.add(ce);
                }
            }

            // Build performance chart data (last 6 assignments)
            List<String> perfLabels = new ArrayList<>();
            List<Integer> perfScores = new ArrayList<>();
            List<Integer> perfAttendance = new ArrayList<>();

            if (assignments != null) {
                List<Assignment> latestAssignments = assignments.stream()
                        .sorted((a, b) -> {
                            if (a.getDueDate() == null) return 1;
                            if (b.getDueDate() == null) return -1;
                            return b.getDueDate().compareTo(a.getDueDate());
                        })
                        .limit(6)
                        .collect(Collectors.toList());

                for (Assignment a : latestAssignments) {
                    perfLabels.add(a.getTitle() != null ? a.getTitle() : "A#" + a.getAssignmentId());
                    List<Submission> aSubs = submissions.stream()
                            .filter(s -> s.getAssignment() != null && s.getAssignment().getAssignmentId().equals(a.getAssignmentId()))
                            .filter(s -> s.getScore() != null)
                            .collect(Collectors.toList());
                    int avgScore = aSubs.isEmpty() ? 0 : (int) Math.round(aSubs.stream().mapToInt(Submission::getScore).average().orElse(0));
                    perfScores.add(avgScore);

                    Integer courseId = a.getCourse() != null ? a.getCourse().getCourseId() : null;
                    int attPct = courseId != null ? analyticsService.getCourseAttendancePercentage(courseId) : 0;
                    perfAttendance.add(attPct);
                }
            }

            model.addAttribute("totalCourses", totalCourses);
            model.addAttribute("activeCourses", activeCourses);
            model.addAttribute("totalStudents", totalStudents);
            model.addAttribute("totalAssignments", totalAssignments);
            model.addAttribute("pendingAssignments", pendingAssignments);
            model.addAttribute("attendancePercentage", attendancePercentage);
            model.addAttribute("recentAssignments", recentAssignments);
            model.addAttribute("courseEngagement", courseEngagement);
            model.addAttribute("perfLabels", perfLabels);
            model.addAttribute("perfScores", perfScores);
            model.addAttribute("perfAttendance", perfAttendance);
        } catch (Exception e) {
            System.err.println("Database fetch failure on dashboard: " + e.getMessage());
            model.addAttribute("courses",  Collections.emptyList());
            model.addAttribute("students", Collections.emptyList());
            model.addAttribute("totalCourses", 0);
            model.addAttribute("activeCourses", 0);
            model.addAttribute("totalStudents", 0);
            model.addAttribute("totalAssignments", 0);
            model.addAttribute("pendingAssignments", 0);
            model.addAttribute("attendancePercentage", 0);
            model.addAttribute("courseEngagement", Collections.emptyList());
            model.addAttribute("recentAssignments", Collections.emptyList());
            model.addAttribute("perfLabels", Collections.emptyList());
            model.addAttribute("perfScores", Collections.emptyList());
            model.addAttribute("perfAttendance", Collections.emptyList());
        }
        return "Admin/admin-dashboard";
    }

    /** Courses — needs full course list for cards */
    @GetMapping("/web/admin/courses")
    public String adminCourses(Model model) {
        try {
            var courses = courseService.getAllCourses();
            model.addAttribute("courses", courses != null ? courses : Collections.emptyList());
        } catch (Exception e) {
            System.err.println("Database fetch failure on admin courses page: " + e.getMessage());
            model.addAttribute("courses", Collections.emptyList());
        }
        return "Admin/admin-course";
    }

    /** Course View Content — Handles the panel workspace redirection for Administrators */
    @GetMapping("/web/admin/courses/content")
    public String adminCourseContent(@RequestParam(required = false) Integer courseId, Model model) {
        if (courseId != null) {
            try {
                var course = courseService.getCourseDetails(courseId);
                course.ifPresent(c -> model.addAttribute("course", c));
            } catch (Exception e) {
                System.err.println("Error loading course: " + e.getMessage());
            }
        }
        return "course-content";
    }

    /** Assignment Preview — Admin only, fetches assignment details by ID */
    @GetMapping("/web/admin/assignments/preview/{assignmentId}")
    public String assignmentPreview(@PathVariable Long assignmentId, Model model) {
        // We pass the ID to the model so the template knows what to fetch
        model.addAttribute("assignmentId", assignmentId);
        return "Admin/assignment-preview";
    }

    /** Students — needs full student list for table */
    @GetMapping("/web/admin/students")
    public String adminStudents(Model model) {
        try {
            var students = studentService.getAllStudents();
            // Refresh status for all students before displaying
            for (var student : students) {
                try {
                    studentService.refreshStudentStatus(student.getStudentId());
                } catch (Exception ignored) {}
            }
            // Re-fetch after status update
            students = studentService.getAllStudents();
            model.addAttribute("students", students != null ? students : Collections.emptyList());
        } catch (Exception e) {
            System.err.println("Database fetch failure on admin students page: " + e.getMessage());
            model.addAttribute("students", Collections.emptyList());
        }
        return "Admin/admin-students";
    }

    /** Profile — passes courses + students for the system overview stats */
    @GetMapping("/web/admin/profile")
    public String adminProfile(Model model) {
        try {
            var courses = courseService.getAllCourses();
            var students = studentService.getAllStudents();
            // Refresh status for all students
            for (var student : students) {
                try {
                    studentService.refreshStudentStatus(student.getStudentId());
                } catch (Exception ignored) {}
            }
            // Re-fetch after status update
            students = studentService.getAllStudents();
            var assignments = assignmentService.getAllAssignments();
            int attendancePercentage = (int) analyticsService.getOverallAttendance().getOrDefault("percentage", 0);

            model.addAttribute("courses",  courses != null ? courses : Collections.emptyList());
            model.addAttribute("students", students != null ? students : Collections.emptyList());
            model.addAttribute("totalAssignments", assignments != null ? assignments.size() : 0);
            model.addAttribute("attendancePercentage", attendancePercentage);
        } catch (Exception e) {
            System.err.println("Database fetch failure on admin profile page: " + e.getMessage());
            model.addAttribute("courses",  Collections.emptyList());
            model.addAttribute("students", Collections.emptyList());
            model.addAttribute("totalAssignments", 0);
            model.addAttribute("attendancePercentage", 0);
        }
        return "Admin/admin-profile";
    }

    /** Admin Settings page */
    @GetMapping("/web/admin/settings")
    public String adminSettings() { return "Admin/admin-settings"; }

    /** Student analytics profile — called from students table Profile button */
    @GetMapping("/web/admin/students/analytics/{studentId}")
    public String adminStudentAnalytics(@PathVariable Long studentId, Model model) {
        try {
            // Refresh status before displaying analytics
            studentService.refreshStudentStatus(studentId.intValue());
            model.addAttribute("student", studentService.getStudentById(studentId));
        } catch (Exception e) {
            System.err.println("Database fetch failure looking up student ID " + studentId + ": " + e.getMessage());
            model.addAttribute("student", null);
        }
        
        return "Admin/student-analytics";
    }

    /** Admin Attendance page */
    @GetMapping("/web/admin/attendance")
    public String adminAttendance() { return "Admin/attendance report"; }

    /** Admin Analytics page */
    @GetMapping("/web/admin/analytics")
    public String adminAnalytics() { return "Admin/admin-analytics"; }

    /** Individual Student Analytics page (admin view) */
    @GetMapping("/web/admin/student-analytics")
    public String adminStudentAnalytics() { return "Admin/student-analytics"; }

    /* ══════════════════════════════════════
       STUDENT PAGES
     ══════════════════════════════════════ */

    @GetMapping("/student/dashboard")
    public String studentDashboard() { return "Student/student-dashboard"; }

    @GetMapping("/student/courses")
    public String studentCourses() { return "Student/student-courses"; }

    /** Course View Content — Handles panel workspace routing for Students */
    @GetMapping("/student/courses/content")
    public String studentCourseContent(@RequestParam(required = false) Integer courseId, Model model) { 
        if (courseId != null) {
            try {
                var course = courseService.getCourseDetails(courseId);
                course.ifPresent(c -> model.addAttribute("course", c));
            } catch (Exception e) {
                System.err.println("Error loading course: " + e.getMessage());
            }
        }
        return "course-content"; 
    }

    @GetMapping("/student/assignments")
    public String studentAssignments() { return "Student/student-assignments"; }

    @GetMapping("/student/attendance")
    public String studentAttendance() { return "Student/student-attendance"; }

    @GetMapping("/student/grades")
    public String studentGrades() { return "Student/student-grades"; }

    @GetMapping("/student/profile")
    public String studentProfile() { return "Student/student-profile"; }

    @GetMapping("/student/analytics")
    public String studentAnalytics() { return "Student/student-analytics"; }

    /* ══════════════════════════════════════
       INSTRUCTOR PAGES
     ══════════════════════════════════════ */

    @GetMapping("/instructor/dashboard")
    public String instructorDashboard() { return "Instructor/instructor-dashboard"; }
}