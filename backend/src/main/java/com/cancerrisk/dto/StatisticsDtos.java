package com.cancerrisk.dto;

import com.cancerrisk.domain.RiskBand;
import java.time.Instant;
import java.util.Map;

public final class StatisticsDtos {

    private StatisticsDtos() {
    }

    public record SummaryResponse(
            long totalPatients,
            long totalAssessments,
            Map<RiskBand, Long> assessmentsByRiskBand,
            long highRiskPatients,
            double averageProbability,
            Instant lastAssessmentAt) {
    }

    public record MonthlyTrendPoint(
            String month,
            long total,
            long low,
            long moderate,
            long high,
            double averageProbability) {
    }

    public record ModelPerformanceResponse(boolean available, Map<String, Object> model) {
    }
}
