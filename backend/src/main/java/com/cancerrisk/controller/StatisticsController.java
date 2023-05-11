package com.cancerrisk.controller;

import com.cancerrisk.dto.StatisticsDtos.ModelPerformanceResponse;
import com.cancerrisk.dto.StatisticsDtos.MonthlyTrendPoint;
import com.cancerrisk.dto.StatisticsDtos.SummaryResponse;
import com.cancerrisk.service.StatisticsService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@Tag(name = "Statistics")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN','DOCTOR','ANALYST')")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/summary")
    public SummaryResponse summary() {
        return statisticsService.summary();
    }

    @GetMapping("/monthly")
    public List<MonthlyTrendPoint> monthly() {
        return statisticsService.monthlyTrend();
    }

    @GetMapping("/model")
    public ModelPerformanceResponse model() {
        return statisticsService.modelPerformance();
    }
}
