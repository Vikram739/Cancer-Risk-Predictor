package com.cancerrisk.mapper;

import com.cancerrisk.client.MlPrediction;
import com.cancerrisk.domain.Assessment;
import com.cancerrisk.domain.FeatureContribution;
import com.cancerrisk.dto.AssessmentDtos.AssessmentResponse;
import com.cancerrisk.dto.AssessmentDtos.FeatureContributionResponse;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface AssessmentMapper {

    @Mapping(target = "patientId", source = "patient.id")
    @Mapping(target = "patientName", source = "patient.fullName")
    @Mapping(target = "patientMrn", source = "patient.mrn")
    AssessmentResponse toResponse(Assessment assessment);

    FeatureContributionResponse toResponse(FeatureContribution contribution);

    FeatureContribution toContribution(MlPrediction.TopFeature feature);

    List<FeatureContribution> toContributions(List<MlPrediction.TopFeature> features);
}
