package com.cancerrisk.service;

import com.cancerrisk.client.MlPrediction;
import com.cancerrisk.client.MlServiceClient;
import com.cancerrisk.domain.Assessment;
import com.cancerrisk.domain.Patient;
import com.cancerrisk.domain.RiskBand;
import com.cancerrisk.dto.AssessmentDtos.AssessmentRequest;
import com.cancerrisk.dto.AssessmentDtos.AssessmentResponse;
import com.cancerrisk.dto.ErrorResponse.FieldViolation;
import com.cancerrisk.dto.PageResponse;
import com.cancerrisk.exception.BadRequestException;
import com.cancerrisk.exception.MlServiceException;
import com.cancerrisk.exception.ResourceNotFoundException;
import com.cancerrisk.mapper.AssessmentMapper;
import com.cancerrisk.repository.AssessmentRepository;
import com.cancerrisk.security.SecurityUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssessmentService {

    private final AssessmentRepository repository;
    private final PatientService patientService;
    private final MlServiceClient mlClient;
    private final AssessmentMapper mapper;
    private final AuditService auditService;
    private final CsvExporter csvExporter;
    private final TransactionTemplate transactionTemplate;

    /**
     * Scores the features with the ML service and stores the result. The remote
     * call happens outside a transaction so a slow model does not pin a DB connection.
     */
    public AssessmentResponse create(AssessmentRequest request) {
        Map<String, Double> features = validateFeatures(request.features());
        Patient patient = patientService.find(request.patientId());
        String user = SecurityUtils.currentUsername();

        MlPrediction prediction = mlClient.predict(features, patient.getMrn());
        RiskBand band = parseBand(prediction.riskBand());

        return transactionTemplate.execute(status -> {
            Assessment assessment = new Assessment();
            assessment.setPatient(patient);
            assessment.setFeatures(features);
            assessment.setProbability(BigDecimal.valueOf(prediction.probability()).setScale(4, RoundingMode.HALF_UP));
            assessment.setRiskBand(band);
            assessment.setPrediction(prediction.prediction());
            assessment.setModelName(prediction.modelName());
            assessment.setModelVersion(prediction.modelVersion());
            assessment.setTopFeatures(mapper.toContributions(
                    prediction.topFeatures() == null ? List.of() : prediction.topFeatures()));
            assessment.setNotes(request.notes());
            assessment.setCreatedBy(user);
            Assessment saved = repository.save(assessment);
            auditService.record(user, "CREATE", "ASSESSMENT", saved.getId(),
                    "patient=" + patient.getId() + ", band=" + band);
            log.info("assessment {} created for patient {} with band {}", saved.getId(), patient.getId(), band);
            return mapper.toResponse(saved);
        });
    }

    @Transactional(readOnly = true)
    public PageResponse<AssessmentResponse> list(RiskBand riskBand, Pageable pageable) {
        var page = riskBand == null
                ? repository.findAllWithPatient(pageable)
                : repository.findByRiskBand(riskBand, pageable);
        return PageResponse.of(page, mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public AssessmentResponse get(Long id) {
        return repository.findWithPatientById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment", id));
    }

    @Transactional(readOnly = true)
    public PageResponse<AssessmentResponse> historyForPatient(Long patientId, Pageable pageable) {
        patientService.find(patientId);
        return PageResponse.of(repository.findByPatientId(patientId, pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public String exportCsv(LocalDate from, LocalDate to, Long patientId) {
        LocalDate start = from == null ? LocalDate.of(2000, 1, 1) : from;
        LocalDate end = to == null ? LocalDate.now(ZoneOffset.UTC) : to;
        if (end.isBefore(start)) {
            throw new BadRequestException("'to' must not be before 'from'");
        }
        Instant fromInstant = start.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = end.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return csvExporter.export(repository.findForExport(fromInstant, toInstant, patientId));
    }

    static Map<String, Double> validateFeatures(Map<String, Double> input) {
        List<FieldViolation> problems = new ArrayList<>();
        Map<String, Double> normalised = new LinkedHashMap<>();
        Map<String, Double> byKey = new LinkedHashMap<>();
        input.forEach((k, v) -> byKey.put(k == null ? "" : k.trim().toLowerCase(Locale.ROOT), v));

        for (String feature : FeatureCatalog.FEATURES) {
            Double value = byKey.remove(feature);
            if (value == null) {
                problems.add(new FieldViolation("features." + feature, "is required"));
            } else if (value.isNaN() || value.isInfinite() || value < 0) {
                problems.add(new FieldViolation("features." + feature, "must be a non negative number"));
            } else {
                normalised.put(feature, value);
            }
        }
        byKey.keySet().forEach(k -> problems.add(new FieldViolation("features." + k, "is not a known feature")));
        if (!problems.isEmpty()) {
            throw new BadRequestException("Invalid feature set", problems);
        }
        return normalised;
    }

    private static RiskBand parseBand(String value) {
        try {
            return RiskBand.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException ex) {
            log.error("ML service returned unknown risk band {}", value);
            throw MlServiceException.unavailable();
        }
    }
}
