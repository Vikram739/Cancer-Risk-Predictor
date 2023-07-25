package com.cancerrisk.service;

import com.cancerrisk.client.MlServiceClient;
import com.cancerrisk.domain.RiskBand;
import com.cancerrisk.dto.StatisticsDtos.ModelPerformanceResponse;
import com.cancerrisk.dto.StatisticsDtos.MonthlyTrendPoint;
import com.cancerrisk.dto.StatisticsDtos.SummaryResponse;
import com.cancerrisk.exception.MlServiceException;
import com.cancerrisk.repository.AssessmentRepository;
import com.cancerrisk.repository.PatientRepository;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final PatientRepository patientRepository;
    private final AssessmentRepository assessmentRepository;
    private final MlServiceClient mlClient;

    @Transactional(readOnly = true)
    public SummaryResponse summary() {
        Map<RiskBand, Long> byBand = new EnumMap<>(RiskBand.class);
        for (RiskBand band : RiskBand.values()) {
            byBand.put(band, 0L);
        }
        assessmentRepository.countByRiskBand().forEach(c -> byBand.put(c.getBand(), c.getTotal()));
        long totalAssessments = byBand.values().stream().mapToLong(Long::longValue).sum();
        Double avg = assessmentRepository.averageProbability();

        return new SummaryResponse(
                patientRepository.count(),
                totalAssessments,
                byBand,
                assessmentRepository.countPatientsWithLatestBand(RiskBand.HIGH.name()),
                avg == null ? 0.0 : Math.round(avg * 10000.0) / 10000.0,
                assessmentRepository.latestCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<MonthlyTrendPoint> monthlyTrend() {
        return assessmentRepository.monthlyTrend().stream()
                .map(m -> new MonthlyTrendPoint(
                        m.getMonth(),
                        m.getTotal(),
                        m.getLow(),
                        m.getModerate(),
                        m.getHigh(),
                        m.getAverageProbability() == null ? 0.0
                                : Math.round(m.getAverageProbability() * 10000.0) / 10000.0))
                .toList();
    }

    public ModelPerformanceResponse modelPerformance() {
        try {
            return new ModelPerformanceResponse(true, mlClient.modelInfo());
        } catch (MlServiceException ex) {
            return new ModelPerformanceResponse(false, Map.of());
        }
    }
}
