package com.project.elms.controller;

import com.project.elms.model.Course;
import com.project.elms.service.CourseService;
import com.project.elms.service.StudentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/web/admin/courses")
public class CourseController {

    private final CourseService courseService;
    private final StudentService studentService;

    public CourseController(CourseService courseService, StudentService studentService) {
        this.courseService = courseService;
        this.studentService = studentService;
    }

   
    @PostMapping("/save")
    public String saveCourse(@ModelAttribute Course course, RedirectAttributes ra) {
        try {
            if (course.getCourseId() == null) {
                courseService.createCourse(course);
                ra.addFlashAttribute("message", "Course created successfully");
            } else {
                courseService.updateCourse(course.getCourseId(), course);
                ra.addFlashAttribute("message", "Course updated successfully");
            }
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("message", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("message", "Failed to save course: " + e.getMessage());
        }
        return "redirect:/web/admin/courses";
    }
//
  // Activate from UI
    @GetMapping("/activate/{id}")
   public String activateCourse(@PathVariable Integer id, RedirectAttributes ra) {
       courseService.activateCourse(id);
       ra.addFlashAttribute("message", "Course activated");
        return "redirect:/web/admin/courses";
}

    @GetMapping("/delete/{id}")
    public String deleteCourse(@PathVariable Integer id, RedirectAttributes ra) {
        courseService.deleteCourse(id);
        ra.addFlashAttribute("message", "Course deleted successfully");
        return "redirect:/web/admin/courses";
    }

    
    @GetMapping("/content/{id}")
    public String viewCourseContent(@PathVariable Integer id, Model model) {
        try {
            Course course = courseService.getCourseDetails(id)
                    .orElseThrow(() -> new RuntimeException("Course not found"));
            model.addAttribute("course", course);
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        return "course-content";
    }
}




