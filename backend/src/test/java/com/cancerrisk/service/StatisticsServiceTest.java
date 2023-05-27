package com.cancerrisk.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.cancerrisk.client.MlServiceClient;
import com.cancerrisk.domain.RiskBand;
import com.cancerrisk.exception.MlServiceException;
import com.cancerrisk.repository.AssessmentRepository;
import com.cancerrisk.repository.AssessmentRepository.RiskBandCount;
import com.cancerrisk.repository.PatientRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    private PatientRepository patientRepository;
    @Mock
    private AssessmentRepository assessmentRepository;
    @Mock
    private MlServiceClient mlClient;
    @InjectMocks
    private StatisticsService service;

    private static RiskBandCount count(RiskBand band, long total) {
        return new RiskBandCount() {
            public RiskBand getBand() {
                return band;
            }

            public long getTotal() {
                return total;
            }
        };
    }

    @Test
    void summaryFillsMissingBandsWithZero() {
        when(patientRepository.count()).thenReturn(12L);
        when(assessmentRepository.countByRiskBand())
                .thenReturn(List.of(count(RiskBand.LOW, 7), count(RiskBand.HIGH, 3)));
        when(assessmentRepository.averageProbability()).thenReturn(0.412345);
        when(assessmentRepository.countPatientsWithLatestBand("HIGH")).thenReturn(2L);

        var summary = service.summary();

        assertThat(summary.totalPatients()).isEqualTo(12);
        assertThat(summary.totalAssessments()).isEqualTo(10);
        assertThat(summary.assessmentsByRiskBand()).containsEntry(RiskBand.MODERATE, 0L);
        assertThat(summary.averageProbability()).isEqualTo(0.4123);
        assertThat(summary.highRiskPatients()).isEqualTo(2);
    }

    @Test
    void modelPerformanceReportsUnavailableInsteadOfFailing() {
        when(mlClient.modelInfo()).thenThrow(MlServiceException.unavailable());
        var response = service.modelPerformance();
        assertThat(response.available()).isFalse();
        assertThat(response.model()).isEmpty();
    }

    @Test
    void modelPerformancePassesInfoThrough() {
        when(mlClient.modelInfo()).thenReturn(Map.of("model_name", "xgboost"));
        assertThat(service.modelPerformance().model()).containsEntry("model_name", "xgboost");
    }
}
