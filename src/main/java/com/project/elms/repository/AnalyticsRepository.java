package com.project.elms.repository;

import com.project.elms.model.AnalyticsSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AnalyticsRepository extends JpaRepository<AnalyticsSnapshot, Integer> {
	
	
	
	List<AnalyticsSnapshot> findByCourse_CourseId(Integer courseId);
	void deleteByCourse_CourseId(Integer courseId);
}