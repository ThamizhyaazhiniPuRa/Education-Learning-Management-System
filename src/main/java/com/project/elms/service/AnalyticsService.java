package com.project.elms.service;

import com.project.elms.model.*;
import com.project.elms.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    @Autowired private AnalyticsRepository analyticsRepo;
    @Autowired private AnalyticsReportRepository analyticsReportRepo;
    @Autowired private SubmissionRepository submissionRepo;
    @Autowired private AttendanceRepository attendanceRepo;
    @Autowired private CourseRepository courseRepo;
    @Autowired private StudentRepository studentRepository;
    @Autowired private AssignmentRepository assignmentRepository;

    public Map<String, Object> getPerformanceReport(Integer courseId) {
        List<Submission> submissions = submissionRepo.findByAssignment_Course_CourseId(courseId);
        DoubleSummaryStatistics stats = submissions.stream()
                .filter(s -> s.getScore() != null)
                .mapToDouble(Submission::getScore)
                .summaryStatistics();

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("averageScore", stats.getCount() > 0 ? Math.round(stats.getAverage()) : 0);
        report.put("highestScore", stats.getCount() > 0 ? (int) stats.getMax() : 0);
        report.put("totalSubmissions", (int) stats.getCount());
        return report;
    }

    public Map<String, Object> getOverallAttendance() {
        List<Attendance> all = attendanceRepo.findAll();
        if (all.isEmpty()) return Map.of("percentage", 0);
        long present = all.stream().filter(a -> a.getStatus() != null && a.getStatus().name().equals("PRESENT")).count();
        int pct = (int) Math.round((double) present / all.size() * 100);
        return Map.of("percentage", pct);
    }

    public Map<String, Object> getStudentAttendance(Integer studentId) {
        List<Attendance> records = attendanceRepo.findByStudent_StudentId(studentId);
        if (records.isEmpty()) return Map.of("percentage", 0);
        long present = records.stream().filter(a -> a.getStatus() != null && a.getStatus().name().equals("PRESENT")).count();
        int pct = (int) Math.round((double) present / records.size() * 100);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("percentage", pct);
        result.put("total", records.size());
        result.put("present", (int) present);
        return result;
    }

    public Map<String, Object> calculateStudentGPA(Integer studentId) {
        Map<String, Object> snapshot = getStudentLearningSnapshot(studentId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("gpa", snapshot.get("gpa4"));
        result.put("cgpa", snapshot.get("cgpa10"));
        result.put("letterGrade", snapshot.get("letterGrade"));
        result.put("totalSubmissions", snapshot.get("gradedSubmissions"));
        result.put("totalCredits", snapshot.get("totalCredits"));
        return result;
    }

    private double toGradePoint10(double pct) {
        if (pct >= 90) return 10.0;
        if (pct >= 80) return 9.0;
        if (pct >= 70) return 8.0;
        if (pct >= 60) return 7.0;
        if (pct >= 50) return 6.0;
        if (pct >= 45) return 5.0;
        if (pct >= 40) return 4.0;
        return 0.0;
    }

    private String toLetterGrade(double cgpa10) {
        if (cgpa10 >= 9.0) return "A+";
        if (cgpa10 >= 8.0) return "A";
        if (cgpa10 >= 7.0) return "B+";
        if (cgpa10 >= 6.0) return "B";
        if (cgpa10 >= 5.0) return "C";
        if (cgpa10 >= 4.0) return "D";
        return "F";
    }

    private String xpStage(int xp) {
        if (xp >= 6000) return "Master";
        if (xp >= 3500) return "Advanced";
        if (xp >= 1800) return "Intermediate";
        if (xp >= 700) return "Explorer";
        return "Beginner";
    }

    private int xpNextMilestone(int xp) {
        if (xp < 700) return 700;
        if (xp < 1800) return 1800;
        if (xp < 3500) return 3500;
        if (xp < 6000) return 6000;
        return xp;
    }

    private Map<String, Object> getStudentLearningSnapshot(Integer studentId) {
        List<Submission> submissions = submissionRepo.findByStudent_StudentId(studentId);
        List<Submission> graded = submissions.stream().filter(s -> s.getScore() != null).collect(Collectors.toList());

        Map<Integer, List<Submission>> byCourse = graded.stream()
                .filter(s -> s.getAssignment() != null && s.getAssignment().getCourse() != null)
                .collect(Collectors.groupingBy(s -> s.getAssignment().getCourse().getCourseId()));

        double weightedPoints = 0.0;
        int totalCredits = 0;
        int totalXp = 0;
        int exemplary = 0, proficient = 0, developing = 0, beginner = 0;

        for (Map.Entry<Integer, List<Submission>> entry : byCourse.entrySet()) {
            List<Submission> courseSubs = entry.getValue();
            Course course = courseSubs.get(0).getAssignment().getCourse();
            int credits = (course != null && course.getCredits() != null && course.getCredits() > 0) ? course.getCredits() : 3;

            double scoreSum = 0.0;
            double maxSum = 0.0;
            for (Submission s : courseSubs) {
                Assignment a = s.getAssignment();
                int max = (a != null && a.getMaxScore() != null && a.getMaxScore() > 0) ? a.getMaxScore() : 100;
                double pct = ((double) s.getScore() / max) * 100.0;
                scoreSum += s.getScore();
                maxSum += max;

                if (pct >= 85) exemplary++;
                else if (pct >= 70) proficient++;
                else if (pct >= 50) developing++;
                else beginner++;

                // XP model: performance + punctuality + quality bump
                int baseXp = (int) Math.round(Math.min(100.0, pct));
                int bonus = s.getScore() >= 90 ? 20 : s.getScore() >= 75 ? 10 : 0;
                if (a != null && a.getDueDate() != null && s.getSubmittedOn() != null && !s.getSubmittedOn().after(a.getDueDate())) {
                    bonus += 10;
                }
                if (s.getFeedback() != null && s.getFeedback().toLowerCase().contains("excellent")) {
                    bonus += 5;
                }
                totalXp += baseXp + bonus;
            }

            double coursePct = maxSum > 0 ? (scoreSum / maxSum) * 100.0 : 0.0;
            double gp10 = toGradePoint10(coursePct);
            weightedPoints += gp10 * credits;
            totalCredits += credits;
        }

        double cgpa10 = totalCredits > 0 ? weightedPoints / totalCredits : 0.0;
        cgpa10 = Math.round(cgpa10 * 100.0) / 100.0;
        double gpa4 = Math.round((cgpa10 / 2.5) * 100.0) / 100.0;

        List<Course> enrolled = courseRepo.findByStudentId(studentId);
        List<Map<String, Object>> continueLearning = new ArrayList<>();
        for (Course c : enrolled) {
            List<Assignment> assignments = assignmentRepository.findByCourse_CourseId(c.getCourseId());
            Set<Long> assignmentIds = assignments.stream().map(Assignment::getAssignmentId).collect(Collectors.toSet());
            long submittedCount = submissions.stream()
                    .map(s -> s.getAssignment() != null ? s.getAssignment().getAssignmentId() : null)
                    .filter(Objects::nonNull)
                    .filter(assignmentIds::contains)
                    .distinct()
                    .count();

            int progress = assignments.isEmpty() ? 0 : (int) Math.round((submittedCount * 100.0) / assignments.size());
            long pending = Math.max(0, assignments.size() - submittedCount);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("courseId", c.getCourseId());
            row.put("title", c.getTitle() != null ? c.getTitle() : "Untitled Course");
            row.put("progress", progress);
            row.put("pending", pending);
            row.put("submitted", submittedCount);
            row.put("totalAssignments", assignments.size());
            continueLearning.add(row);
        }
        continueLearning.sort((a, b) -> Integer.compare(
                ((Number) b.get("progress")).intValue(),
                ((Number) a.get("progress")).intValue()));

        Map<String, Object> rubric = new LinkedHashMap<>();
        rubric.put("exemplary", exemplary);
        rubric.put("proficient", proficient);
        rubric.put("developing", developing);
        rubric.put("beginner", beginner);

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("studentId", studentId);
        snapshot.put("cgpa10", cgpa10);
        snapshot.put("gpa4", gpa4);
        snapshot.put("letterGrade", toLetterGrade(cgpa10));
        snapshot.put("gradedSubmissions", graded.size());
        snapshot.put("totalCredits", totalCredits);
        snapshot.put("xpPoints", totalXp);
        snapshot.put("xpStage", xpStage(totalXp));
        snapshot.put("nextXpMilestone", xpNextMilestone(totalXp));
        snapshot.put("rubric", rubric);
        snapshot.put("continueLearning", continueLearning);
        return snapshot;
    }

    public Map<String, Object> getStudentLearningInsights(Integer studentId) {
        Map<String, Object> snapshot = getStudentLearningSnapshot(studentId);
        // Add current student status to the response
        Student student = studentRepository.findById(studentId).orElse(null);
        if (student != null) {
            snapshot.put("name", student.getName());
            snapshot.put("email", student.getEmail());
            snapshot.put("program", student.getProgram());
            snapshot.put("status", student.getStatus() != null ? student.getStatus().name() : "ACTIVE");
        }
        return snapshot;
    }

    public List<Map<String, Object>> getAllStudentsLearningInsights() {
        List<Student> students = studentRepository.findAllWithUser().stream()
                .filter(s -> s.getRole() == Role.STUDENT)
                .collect(Collectors.toList());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Student s : students) {
            Map<String, Object> snapshot = getStudentLearningSnapshot(s.getStudentId());
            snapshot.put("name", s.getName());
            snapshot.put("program", s.getProgram());
            snapshot.put("status", s.getStatus() != null ? s.getStatus().name() : "ACTIVE");
            rows.add(snapshot);
        }
        rows.sort((a, b) -> Double.compare(
                ((Number) b.get("cgpa10")).doubleValue(),
                ((Number) a.get("cgpa10")).doubleValue()));
        return rows;
    }

    public String exportGradesCSV(Integer courseId) {
        List<Submission> submissions = submissionRepo.findByAssignment_Course_CourseId(courseId);
        StringBuilder sb = new StringBuilder("StudentID,StudentName,AssignmentTitle,Score,MaxScore,Percentage,Grade,Feedback\n");
        for (Submission s : submissions) {
            if (s.getScore() == null) continue;
            String name = s.getStudent() != null ? s.getStudent().getName() : "Unknown";
            String title = s.getAssignment() != null ? s.getAssignment().getTitle() : "Unknown";
            int score = s.getScore();
            int max = s.getAssignment() != null && s.getAssignment().getMaxScore() != null ? s.getAssignment().getMaxScore() : 100;
            double pct = max > 0 ? (double) score / max * 100 : 0;
            String grade = pct >= 90 ? "A+" : pct >= 80 ? "A" : pct >= 70 ? "B+" : pct >= 60 ? "B" : pct >= 50 ? "C" : "F";
            String feedback = s.getFeedback() != null ? s.getFeedback().replace(",", ";") : "";
            int sid = s.getStudent() != null ? s.getStudent().getStudentId() : 0;
            sb.append(sid).append(",").append(name).append(",").append(title).append(",")
              .append(score).append(",").append(max).append(",")
              .append(String.format("%.1f", pct)).append(",").append(grade).append(",").append(feedback).append("\n");
        }
        return sb.toString();
    }

    public AnalyticsSnapshot generateSnapshot(Integer courseId) {
        Course course = courseRepo.findById(courseId).orElseThrow();
        List<Submission> submissions = submissionRepo.findByAssignment_Course_CourseId(courseId);
        double avgScore = submissions.stream().filter(s -> s.getScore() != null).mapToInt(Submission::getScore).average().orElse(0.0);
        List<Attendance> attendance = attendanceRepo.findByCourse_CourseId(courseId);
        long present = attendance.stream().filter(a -> a.getStatus() != null && a.getStatus().name().equals("PRESENT")).count();
        double attRate = attendance.isEmpty() ? 0.0 : (double) present / attendance.size() * 100;
        double engIndex = (avgScore * 0.7) + (attRate * 0.3);

        AnalyticsSnapshot snapshot = new AnalyticsSnapshot();
        snapshot.setCourse(course);
        snapshot.setGeneratedOn(new Date());
        snapshot.setAverageScore(new BigDecimal(avgScore).setScale(2, RoundingMode.HALF_UP));
        snapshot.setAttendanceRate(new BigDecimal(attRate).setScale(2, RoundingMode.HALF_UP));
        snapshot.setEngagementIndex(new BigDecimal(engIndex).setScale(2, RoundingMode.HALF_UP));
        return analyticsRepo.save(snapshot);
    }

    public String exportCSV(Integer courseId) {
        List<AnalyticsSnapshot> snapshots = analyticsRepo.findByCourse_CourseId(courseId);
        StringBuilder sb = new StringBuilder("ID,Date,Score,Attendance,Engagement\n");
        for (AnalyticsSnapshot s : snapshots) {
            sb.append(s.getReportId()).append(",")
              .append(s.getGeneratedOn()).append(",")
              .append(s.getAverageScore()).append(",")
              .append(s.getAttendanceRate()).append(",")
              .append(s.getEngagementIndex()).append("\n");
        }
        return sb.toString();
    }

    public int getCourseAttendancePercentage(Integer courseId) {
        List<Attendance> attendance = attendanceRepo.findByCourse_CourseId(courseId);
        if (attendance.isEmpty()) return 0;
        long present = attendance.stream().filter(a -> a.getStatus() != null && a.getStatus().name().equals("PRESENT")).count();
        return (int) Math.round((double) present / attendance.size() * 100);
    }

    public int getCourseEngagementPercentage(Integer courseId) {
        List<Submission> submissions = submissionRepo.findByAssignment_Course_CourseId(courseId);
        double avgScore = submissions.stream()
                .filter(s -> s.getScore() != null)
                .mapToInt(Submission::getScore)
                .average()
                .orElse(0.0);
        int attendancePct = getCourseAttendancePercentage(courseId);
        double engagement = (avgScore * 0.7) + (attendancePct * 0.3);
        return Math.min(100, (int) Math.round(engagement));
    }

    public List<Map<String, Object>> getStudentProgressSummaries() {
        List<Student> students = studentRepository.findAllWithUser().stream()
                .filter(s -> s.getRole() == Role.STUDENT)
                .collect(Collectors.toList());

        List<Map<String, Object>> result = new ArrayList<>();
        for (Student s : students) {
            List<Assignment> assignments = assignmentRepository.findAssignmentsByStudentId(s.getStudentId());
            int total = assignments.size();
            Set<Long> assignmentIds = assignments.stream()
                    .map(Assignment::getAssignmentId)
                    .collect(Collectors.toSet());

            List<Submission> subs = submissionRepo.findByStudent_StudentId(s.getStudentId());
            long submitted = subs.stream()
                    .map(sub -> sub.getAssignment() != null ? sub.getAssignment().getAssignmentId() : null)
                    .filter(Objects::nonNull)
                    .filter(assignmentIds::contains)
                    .distinct()
                    .count();

            int progress = total > 0 ? (int) Math.round((double) submitted / total * 100) : 0;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("studentId", s.getStudentId());
            row.put("name", s.getName());
            row.put("program", s.getProgram());
            row.put("status", s.getStatus() != null ? s.getStatus().name() : "ACTIVE");
            row.put("submitted", (int) submitted);
            row.put("total", total);
            row.put("progress", progress);
            result.add(row);
        }
        return result;
    }

    public Map<String, Object> getAdminAnalyticsOverview() {
        List<Course> courses = courseRepo.findAll();
        int attendancePct = ((Number) getOverallAttendance().getOrDefault("percentage", 0)).intValue();

        List<Map<String, Object>> reports = new ArrayList<>();
        List<Integer> avgScores = new ArrayList<>();
        int totalSubmissions = 0;

        // Course status distribution
        int activeCourses = 0;
        int inactiveCourses = 0;
        int coursesWithSubmissions = 0;
        int coursesWithoutSubmissions = 0;

        for (Course c : courses) {
            List<Submission> submissions = submissionRepo.findByAssignment_Course_CourseId(c.getCourseId());
            List<Submission> graded = submissions.stream()
                    .filter(s -> s.getScore() != null)
                    .collect(Collectors.toList());

            int avg = graded.isEmpty()
                    ? 0
                    : (int) Math.round(graded.stream().mapToInt(Submission::getScore).average().orElse(0));

            totalSubmissions += submissions.size();
            avgScores.add(avg);

            // Track course status
            if (c.getStatus() != null && c.getStatus() == Course.CourseStatus.ACTIVE) {
                activeCourses++;
            } else if (c.getStatus() != null && c.getStatus() == Course.CourseStatus.INACTIVE) {
                inactiveCourses++;
            } else {
                // Default to active if status is null
                activeCourses++;
            }

            // Track submission activity
            if (!submissions.isEmpty()) {
                coursesWithSubmissions++;
            } else {
                coursesWithoutSubmissions++;
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("courseId", c.getCourseId());
            row.put("title", c.getTitle() != null ? c.getTitle() : "Untitled Course");
            row.put("averageScore", avg);
            row.put("totalSubmissions", submissions.size());
            row.put("gradedSubmissions", graded.size());
            row.put("status", c.getStatus() != null ? c.getStatus().name() : "ACTIVE");
            reports.add(row);
        }

        int avgAll = avgScores.isEmpty()
                ? 0
                : (int) Math.round(avgScores.stream().mapToInt(Integer::intValue).average().orElse(0));

        int median = 0;
        if (!avgScores.isEmpty()) {
            List<Integer> sorted = new ArrayList<>(avgScores);
            Collections.sort(sorted);
            int mid = sorted.size() / 2;
            median = sorted.size() % 2 == 0
                    ? (int) Math.round((sorted.get(mid - 1) + sorted.get(mid)) / 2.0)
                    : sorted.get(mid);
        }

        Map<String, Object> top = reports.stream()
                .max(Comparator.comparingInt(r -> ((Number) r.get("averageScore")).intValue()))
                .orElse(null);
        Map<String, Object> low = reports.stream()
                .min(Comparator.comparingInt(r -> ((Number) r.get("averageScore")).intValue()))
                .orElse(null);

        // Get student statistics
        List<Student> students = studentRepository.findAllWithUser().stream()
                .filter(s -> s.getRole() == Role.STUDENT)
                .collect(Collectors.toList());
        
        int activeStudents = 0;
        int graduatedStudents = 0;
        int droppedStudents = 0;
        
        for (Student s : students) {
            if (s.getStatus() == null || s.getStatus() == Student.StudentStatus.ACTIVE) {
                activeStudents++;
            } else if (s.getStatus() == Student.StudentStatus.GRADUATED) {
                graduatedStudents++;
            } else if (s.getStatus() == Student.StudentStatus.DROPPED) {
                droppedStudents++;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalCourses", courses.size());
        result.put("totalSubmissions", totalSubmissions);
        result.put("attendancePercentage", attendancePct);
        result.put("averageScore", avgAll);
        result.put("medianScore", median);
        result.put("topCourse", top);
        result.put("lowestCourse", low);
        result.put("healthIndex", (int) Math.round((avgAll + attendancePct) / 2.0));
        result.put("courseReports", reports);
        
        // Course distribution data
        result.put("activeCourses", activeCourses);
        result.put("inactiveCourses", inactiveCourses);
        result.put("coursesWithSubmissions", coursesWithSubmissions);
        result.put("coursesWithoutSubmissions", coursesWithoutSubmissions);
        
        // Student statistics
        result.put("totalStudents", students.size());
        result.put("activeStudents", activeStudents);
        result.put("graduatedStudents", graduatedStudents);
        result.put("droppedStudents", droppedStudents);

        double totalCgpa = 0.0;
        int totalXp = 0;
        int cgpaCount = 0;
        for (Student s : students) {
            Map<String, Object> snap = getStudentLearningSnapshot(s.getStudentId());
            totalCgpa += ((Number) snap.getOrDefault("cgpa10", 0.0)).doubleValue();
            totalXp += ((Number) snap.getOrDefault("xpPoints", 0)).intValue();
            cgpaCount++;
        }
        result.put("avgCgpa", cgpaCount > 0 ? Math.round((totalCgpa / cgpaCount) * 100.0) / 100.0 : 0.0);
        result.put("totalXpPoints", totalXp);
        
        return result;
    }
    
    public AnalyticsReport saveAnalyticsExport(String reportType, String csvData, String generatedBy, Map<String, Object> overview) {
        AnalyticsReport report = new AnalyticsReport();
        report.setReportType(reportType);
        report.setReportData(csvData);
        report.setGeneratedBy(generatedBy);
        report.setGeneratedAt(new Date());
        
        if (overview != null) {
            report.setAvgScore(((Number) overview.getOrDefault("averageScore", 0)).intValue());
            report.setAttendanceRate(((Number) overview.getOrDefault("attendancePercentage", 0)).intValue());
            report.setTotalSubmissions(((Number) overview.getOrDefault("totalSubmissions", 0)).intValue());
            report.setTotalCourses(((Number) overview.getOrDefault("totalCourses", 0)).intValue());
            report.setHealthIndex(((Number) overview.getOrDefault("healthIndex", 0)).intValue());
        }
        
        return analyticsReportRepo.save(report);
    }
    
    public List<AnalyticsReport> getRecentReports() {
        return analyticsReportRepo.findTop10ByOrderByGeneratedAtDesc();
    }
    
    /**
     * Generate snapshots for all courses and return summary
     */
    public Map<String, Object> generateAllSnapshots() {
        List<Course> courses = courseRepo.findAll();
        List<AnalyticsSnapshot> snapshots = new ArrayList<>();
        
        for (Course c : courses) {
            try {
                AnalyticsSnapshot snapshot = generateSnapshot(c.getCourseId());
                snapshots.add(snapshot);
            } catch (Exception e) {
                // Log and continue
            }
        }
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("generatedCount", snapshots.size());
        result.put("totalCourses", courses.size());
        result.put("generatedAt", new Date());
        result.put("snapshots", snapshots);
        return result;
    }
    
    /**
     * Get all analytics snapshots with pagination support
     */
    public List<AnalyticsSnapshot> getAllSnapshots() {
        return analyticsRepo.findAll();
    }
    
    /**
     * Get analytics trends over time for a specific course
     */
    public Map<String, Object> getCourseTrends(Integer courseId) {
        List<AnalyticsSnapshot> snapshots = analyticsRepo.findByCourse_CourseId(courseId);
        Course course = courseRepo.findById(courseId).orElse(null);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("courseId", courseId);
        result.put("courseTitle", course != null ? course.getTitle() : "Unknown");
        result.put("snapshotCount", snapshots.size());
        
        if (!snapshots.isEmpty()) {
            // Sort by date
            snapshots.sort(Comparator.comparing(AnalyticsSnapshot::getGeneratedOn));
            
            List<Map<String, Object>> trendData = new ArrayList<>();
            for (AnalyticsSnapshot s : snapshots) {
                Map<String, Object> point = new LinkedHashMap<>();
                point.put("date", s.getGeneratedOn());
                point.put("averageScore", s.getAverageScore());
                point.put("attendanceRate", s.getAttendanceRate());
                point.put("engagementIndex", s.getEngagementIndex());
                trendData.add(point);
            }
            result.put("trends", trendData);
            
            // Calculate improvements
            if (snapshots.size() >= 2) {
                AnalyticsSnapshot first = snapshots.get(0);
                AnalyticsSnapshot last = snapshots.get(snapshots.size() - 1);
                
                double scoreImprovement = last.getAverageScore().subtract(first.getAverageScore()).doubleValue();
                double attendanceImprovement = last.getAttendanceRate().subtract(first.getAttendanceRate()).doubleValue();
                
                result.put("scoreImprovement", Math.round(scoreImprovement * 100.0) / 100.0);
                result.put("attendanceImprovement", Math.round(attendanceImprovement * 100.0) / 100.0);
            }
        } else {
            result.put("trends", new ArrayList<>());
        }
        
        return result;
    }
    
    /**
     * Get comparative analytics across all courses
     */
    public Map<String, Object> getComparativeAnalytics() {
        List<Course> courses = courseRepo.findAll();
        
        List<Map<String, Object>> courseComparisons = new ArrayList<>();
        double totalAvgScore = 0;
        double totalAttendance = 0;
        int courseCount = 0;
        
        for (Course c : courses) {
            Map<String, Object> perf = getPerformanceReport(c.getCourseId());
            int attendance = getCourseAttendancePercentage(c.getCourseId());
            int engagement = getCourseEngagementPercentage(c.getCourseId());
            
            int avgScore = ((Number) perf.getOrDefault("averageScore", 0)).intValue();
            int totalSubs = ((Number) perf.getOrDefault("totalSubmissions", 0)).intValue();
            
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("courseId", c.getCourseId());
            row.put("title", c.getTitle());
            row.put("status", c.getStatus() != null ? c.getStatus().name() : "ACTIVE");
            row.put("averageScore", avgScore);
            row.put("attendanceRate", attendance);
            row.put("engagementIndex", engagement);
            row.put("totalSubmissions", totalSubs);
            row.put("credits", c.getCredits());
            
            courseComparisons.add(row);
            totalAvgScore += avgScore;
            totalAttendance += attendance;
            courseCount++;
        }
        
        // Sort by engagement (best performing first)
        courseComparisons.sort((a, b) -> 
            ((Number) b.get("engagementIndex")).intValue() - ((Number) a.get("engagementIndex")).intValue());
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalCourses", courseCount);
        result.put("overallAverageScore", courseCount > 0 ? Math.round(totalAvgScore / courseCount) : 0);
        result.put("overallAttendance", courseCount > 0 ? Math.round(totalAttendance / courseCount) : 0);
        result.put("courseComparisons", courseComparisons);
        
        // Top and bottom performers
        if (!courseComparisons.isEmpty()) {
            result.put("topPerformer", courseComparisons.get(0));
            result.put("needsAttention", courseComparisons.get(courseComparisons.size() - 1));
        }
        
        return result;
    }
}