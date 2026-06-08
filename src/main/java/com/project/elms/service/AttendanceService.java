package com.project.elms.service;
 
import com.project.elms.model.Attendance;
import com.project.elms.model.Course;
import com.project.elms.model.Student;
import com.project.elms.repository.AttendanceRepository;
import com.project.elms.repository.CourseRepository;
import com.project.elms.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class AttendanceService {
    @Autowired private AttendanceRepository attendanceRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private CourseRepository courseRepository;
 
    public Attendance markAttendance(Attendance attendance) {
    	attendance.setDate(new Date());
        return attendanceRepository.save(attendance);
    }

    public List<Attendance> getSummary(Integer studentId, Integer courseId) {
        return attendanceRepository.findByStudent_StudentIdAndCourse_CourseId(studentId, courseId);
    }

    public List<Attendance> getByStudent(Integer studentId) {
        return attendanceRepository.findByStudent_StudentId(studentId);
    }

    public void recordParticipation(Integer studentId, Integer courseId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        Attendance attendance = new Attendance();
        attendance.setStudent(student);
        attendance.setCourse(course);
        attendance.setDate(new Date());
        attendance.setStatus(Attendance.AttendanceStatus.PRESENT);
        attendanceRepository.save(attendance);
    }
}