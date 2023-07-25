package com.cancerrisk.dto;

import com.cancerrisk.domain.RiskBand;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class AssessmentDtos {

    private AssessmentDtos() {
    }

    public record AssessmentRequest(
            @NotNull Long patientId,
            @NotEmpty Map<String, @NotNull @PositiveOrZero Double> features,
            @Size(max = 1000) String notes) {
    }

    public record FeatureContributionResponse(String feature, double value, double shapValue, String direction) {
    }

    public record AssessmentResponse(
            Long id,
            Long patientId,
            String patientName,
            String patientMrn,
            BigDecimal probability,
            RiskBand riskBand,
            String prediction,
            String modelName,
            String modelVersion,
            List<FeatureContributionResponse> topFeatures,
            Map<String, Double> features,
            String notes,
            String createdBy,
            Instant createdAt) {
    }
}
