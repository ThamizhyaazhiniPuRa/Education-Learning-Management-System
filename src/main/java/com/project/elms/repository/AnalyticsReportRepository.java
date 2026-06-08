package com.project.elms.repository;

import com.project.elms.model.AnalyticsReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AnalyticsReportRepository extends JpaRepository<AnalyticsReport, Long> {
    List<AnalyticsReport> findByReportTypeOrderByGeneratedAtDesc(String reportType);
    List<AnalyticsReport> findTop10ByOrderByGeneratedAtDesc();
}
