package com.project.elms.service;
import com.project.elms.model.*;
import com.project.elms.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class AssignmentService {

    @Autowired
    private AssignmentRepository assignmentRepository;
   

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    @Lazy
    private StudentService studentService;

    public Assignment createAssignment(Assignment assignment, Integer courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course ID: " + courseId + " not found."));
        assignment.setCourse(course);
        if (assignment.getDueDate() == null) {
            assignment.setDueDate(new Date());
        }
        return assignmentRepository.save(assignment);
    }

    @Transactional
    public Submission submitAssignment(Submission submission) {
        Assignment assignment = assignmentRepository.findById(submission.getAssignment().getAssignmentId())
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        Student student = studentRepository.findById(submission.getStudent().getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found"));
        submission.setAssignment(assignment);
        submission.setStudent(student);
        submission.setSubmittedOn(new Date());
        return submissionRepository.save(submission);
    }

    public Submission gradeAssignment(Long submissionId, Integer score, String feedback) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));
        submission.setScore(score);
        submission.setFeedback(feedback);
        return submissionRepository.save(submission);
    }

    public List<Assignment> getAllAssignments() {
        return assignmentRepository.findAll();
    }

    public Assignment getAssignmentById(Long assignmentId) {
        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment ID: " + assignmentId + " not found."));
    }

    public List<Submission> getGradesByAssignment(Long assignmentId) {
        return submissionRepository.findByAssignment_AssignmentId(assignmentId);
    }

    public List<Assignment> getAssignmentsByCourse(Integer courseId) {
        return assignmentRepository.findByCourse_CourseId(courseId);
    }

    public List<Submission> getSubmissionsByStudent(Integer studentId) {
        return submissionRepository.findByStudent_StudentId(studentId);
    }

    public Map<String, Object> getCertificateStatus(Integer courseId, Integer studentId) {
        List<Assignment> courseAssignments = assignmentRepository.findByCourse_CourseId(courseId);
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        if (courseAssignments.isEmpty()) {
            result.put("allPassed", true);
            result.put("total", 0);
            result.put("passed", 0);
            result.put("grade", "N/A");
            result.put("avgScore", 0);
            result.put("locked", false);
            return result;
        }
        int passed = 0, totalScore = 0, totalMax = 0;
        for (Assignment a : courseAssignments) {
            java.util.Optional<Submission> best = submissionRepository
                    .findTopByAssignment_AssignmentIdAndStudent_StudentIdOrderByRetakeCountDesc(
                            a.getAssignmentId(), studentId);
            if (best.isPresent() && best.get().getScore() != null && best.get().getScore() >= 50) {
                passed++;
                totalScore += best.get().getScore();
                totalMax   += (a.getMaxScore() != null && a.getMaxScore() > 0) ? a.getMaxScore() : 100;
            }
        }
        boolean allPassed = passed == courseAssignments.size();
        double avgPct = totalMax > 0 ? (double) totalScore / totalMax * 100 : 0;
        String grade = computeGrade((int) Math.round(avgPct));
        result.put("allPassed", allPassed);
        result.put("total", courseAssignments.size());
        result.put("passed", passed);
        result.put("grade", grade);
        result.put("avgScore", (int) Math.round(avgPct));
        result.put("locked", !allPassed);
        return result;
    }

    public boolean hasSubmitted(Long assignmentId, Integer studentId) {
        return submissionRepository.existsByAssignment_AssignmentIdAndStudent_StudentId(assignmentId, studentId);
    }

    @Transactional
    public Map<String, Object> submitQuiz(Long assignmentId, Integer studentId, Integer score, String answer) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (assignment.getDueDate() != null) {
            LocalDate dueDate = assignment.getDueDate().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate today   = LocalDate.now(ZoneId.systemDefault());
            if (today.isAfter(dueDate)) {
                return Map.of("status", "past_due",
                        "message", "This assignment is past its due date and can no longer be submitted.");
            }
        }

        long attemptCount = submissionRepository.countByAssignment_AssignmentIdAndStudent_StudentId(assignmentId, studentId);
        java.util.Optional<Submission> latestOpt = submissionRepository
                .findTopByAssignment_AssignmentIdAndStudent_StudentIdOrderByRetakeCountDesc(assignmentId, studentId);

        int prevScore  = latestOpt.map(s -> s.getScore() != null ? s.getScore() : 0).orElse(0);
        boolean prevPassed = prevScore >= 50;

        if (prevPassed) {
            return Map.of("status", "already_submitted", "message", "You have already passed this quiz.");
        }
        if (attemptCount >= 3) {
            return Map.of("status", "max_retakes", "message", "Maximum retake limit (2) reached for this quiz.");
        }

        String grade = computeGrade(score);
        String feedback;
        if      (score >= 90) feedback = "Excellent! Grade: A+";
        else if (score >= 80) feedback = "Great work! Grade: A";
        else if (score >= 70) feedback = "Good job! Grade: B+";
        else if (score >= 60) feedback = "Decent effort! Grade: B";
        else if (score >= 50) feedback = "Passed! Grade: C";
        else feedback = "Failed. Grade: F — " + (attemptCount < 2 ? "You may retake this quiz."
                : (attemptCount == 2 ? "1 retake remaining." : "No more retakes allowed."));

        Submission sub = new Submission();
        sub.setAssignment(assignment);
        sub.setStudent(student);
        sub.setScore(score);
        sub.setAnswer(answer);
        sub.setSubmittedOn(new Date());
        sub.setFeedback(feedback);
        sub.setRetakeCount((int) attemptCount);
        submissionRepository.save(sub);

        if (score >= 50) {
            try { studentService.refreshStudentStatus(studentId); } catch (Exception ignored) {}
        }

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("status", score >= 50 ? "ok" : "failed");
        result.put("score", score);
        result.put("grade", grade);
        result.put("feedback", feedback);
        result.put("retakeCount", (int) attemptCount);
        result.put("retriesLeft", score < 50 ? Math.max(0, 2 - (int) attemptCount) : 0);
        return result;
    }

    private String computeGrade(int score) {
        if (score >= 90) return "A+";
        if (score >= 80) return "A";
        if (score >= 70) return "B+";
        if (score >= 60) return "B";
        if (score >= 50) return "C";
        return "F";
    }

    public Map<String, Object> getRetakeInfo(Long assignmentId, Integer studentId) {
        long count = submissionRepository.countByAssignment_AssignmentIdAndStudent_StudentId(assignmentId, studentId);
        java.util.Optional<Submission> latest = submissionRepository
                .findTopByAssignment_AssignmentIdAndStudent_StudentIdOrderByRetakeCountDesc(assignmentId, studentId);
        int latestScore = latest.map(s -> s.getScore() != null ? s.getScore() : 0).orElse(0);
        boolean passed  = latestScore >= 50;
        Map<String, Object> info = new java.util.LinkedHashMap<>();
        info.put("attempts",    count);
        info.put("latestScore", latestScore);
        info.put("grade",       count > 0 ? computeGrade(latestScore) : "N/A");
        info.put("passed",      passed);
        info.put("retriesLeft", passed ? 0 : Math.max(0, 2 - (int)(count == 0 ? 0 : count - 1)));
        info.put("maxRetakes",  2);
        return info; }
    
    @Transactional
    public Map<String, Object> extendDueDate(Long assignmentId, Date newDueDate) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment ID: " + assignmentId + " not found."));
 LocalDate newDue = newDueDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate today  = LocalDate.now(ZoneId.systemDefault());
        if (!newDue.isAfter(today)) {
            throw new RuntimeException("New due date must be a future date (after today: " + today + ").");}
        Date oldDueDate = assignment.getDueDate();
        assignment.setDueDate(newDueDate);
        assignmentRepository.save(assignment);
        Integer courseId = assignment.getCourse().getCourseId();
        
        try {
            studentService.refreshAllStudentsInCourse(courseId);
        } 
        catch (Exception e) {
            System.err.println("Warning: batch status refresh after extend-due-date: " + e.getMessage());}
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("status",      "extended");
        result.put("assignmentId", assignmentId);
        result.put("oldDueDate",   oldDueDate != null
                ? oldDueDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toString() : null);
        result.put("newDueDate",   newDue.toString());
        result.put("message",      "Due date extended successfully. All enrolled students can now submit.");
        return result;
    }
}