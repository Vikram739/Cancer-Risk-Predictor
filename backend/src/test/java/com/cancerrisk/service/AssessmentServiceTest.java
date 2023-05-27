package com.cancerrisk.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cancerrisk.client.MlServiceClient;
import com.cancerrisk.domain.Assessment;
import com.cancerrisk.domain.RiskBand;
import com.cancerrisk.dto.AssessmentDtos.AssessmentRequest;
import com.cancerrisk.dto.AssessmentDtos.AssessmentResponse;
import com.cancerrisk.exception.BadRequestException;
import com.cancerrisk.exception.MlServiceException;
import com.cancerrisk.exception.ResourceNotFoundException;
import com.cancerrisk.mapper.AssessmentMapper;
import com.cancerrisk.mapper.AssessmentMapperImpl;
import com.cancerrisk.repository.AssessmentRepository;
import com.cancerrisk.support.TestData;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceTest {

    @Mock
    private AssessmentRepository repository;
    @Mock
    private PatientService patientService;
    @Mock
    private MlServiceClient mlClient;
    @Mock
    private AuditService auditService;
    @Mock
    private TransactionTemplate transactionTemplate;

    private final AssessmentMapper mapper = new AssessmentMapperImpl();
    private AssessmentService service;

    @BeforeEach
    void setUp() {
        service = new AssessmentService(repository, patientService, mlClient, mapper, auditService,
                new CsvExporter(), transactionTemplate);
    }

    @SuppressWarnings("unchecked")
    private void runTransactionsInline() {
        when(transactionTemplate.execute(any())).thenAnswer(inv ->
                ((TransactionCallback<Object>) inv.getArgument(0)).doInTransaction(null));
    }

    @Test
    void createStoresPredictionFromMlService() {
        runTransactionsInline();
        when(patientService.find(7L)).thenReturn(TestData.patient(7L, "MRN-7"));
        when(mlClient.predict(anyMap(), eq("MRN-7"))).thenReturn(TestData.prediction(0.91234, "HIGH"));
        when(repository.save(any(Assessment.class))).thenAnswer(inv -> {
            Assessment a = inv.getArgument(0);
            a.setId(99L);
            return a;
        });

        AssessmentResponse response = service.create(new AssessmentRequest(7L, TestData.features(), "first visit"));

        ArgumentCaptor<Assessment> saved = ArgumentCaptor.forClass(Assessment.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getProbability()).isEqualByComparingTo(new BigDecimal("0.9123"));
        assertThat(saved.getValue().getRiskBand()).isEqualTo(RiskBand.HIGH);
        assertThat(saved.getValue().getTopFeatures()).hasSize(2);
        assertThat(response.id()).isEqualTo(99L);
        assertThat(response.patientMrn()).isEqualTo("MRN-7");
        assertThat(response.topFeatures().get(0).feature()).isEqualTo("worst_radius");
        verify(auditService).record(anyString(), eq("CREATE"), eq("ASSESSMENT"), eq(99L), anyString());
    }

    @Test
    void createRejectsIncompleteFeaturesBeforeCallingMl() {
        Map<String, Double> features = new HashMap<>(TestData.features());
        features.remove("worst_area");
        features.put("tumour_colour", 2.0);

        assertThatThrownBy(() -> service.create(new AssessmentRequest(1L, features, null)))
                .isInstanceOf(BadRequestException.class)
                .satisfies(ex -> assertThat(((BadRequestException) ex).getDetails())
                        .extracting("field")
                        .containsExactlyInAnyOrder("features.worst_area", "features.tumour_colour"));
        verify(mlClient, never()).predict(anyMap(), any());
    }

    @Test
    void createPropagatesMlOutageWithoutSaving() {
        when(patientService.find(1L)).thenReturn(TestData.patient(1L, "MRN-1"));
        when(mlClient.predict(anyMap(), any())).thenThrow(MlServiceException.unavailable());

        assertThatThrownBy(() -> service.create(new AssessmentRequest(1L, TestData.features(), null)))
                .isInstanceOf(MlServiceException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void validateFeaturesNormalisesKeyCase() {
        Map<String, Double> features = new HashMap<>();
        TestData.features().forEach((k, v) -> features.put(k.toUpperCase(), v));
        assertThat(AssessmentService.validateFeatures(features)).hasSize(30).containsKey("mean_radius");
    }

    @Test
    void validateFeaturesRejectsNegativeValues() {
        Map<String, Double> features = new HashMap<>(TestData.features());
        features.put("mean_area", -3.0);
        assertThatThrownBy(() -> AssessmentService.validateFeatures(features))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void getThrowsWhenMissing() {
        when(repository.findWithPatientById(5L)).thenReturn(java.util.Optional.empty());
        assertThatThrownBy(() -> service.get(5L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void exportRejectsInvertedRange() {
        assertThatThrownBy(() -> service.exportCsv(java.time.LocalDate.of(2023, 5, 1),
                java.time.LocalDate.of(2023, 4, 1), null))
                .isInstanceOf(BadRequestException.class);
    }
}
