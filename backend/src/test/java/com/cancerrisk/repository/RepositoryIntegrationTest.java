package com.cancerrisk.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.cancerrisk.domain.Assessment;
import com.cancerrisk.domain.FeatureContribution;
import com.cancerrisk.domain.Patient;
import com.cancerrisk.domain.RiskBand;
import com.cancerrisk.domain.Sex;
import com.cancerrisk.support.AbstractIntegrationTest;
import com.cancerrisk.support.TestData;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class RepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private AssessmentRepository assessmentRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EntityManager entityManager;

    private Patient newPatient(String mrn) {
        Patient p = new Patient();
        p.setMrn(mrn);
        p.setFirstName("Zoya");
        p.setLastName("Quarterman");
        p.setDateOfBirth(LocalDate.of(1969, 8, 30));
        p.setSex(Sex.FEMALE);
        return patientRepository.save(p);
    }

    @Test
    void seedMigrationLoadsUsersWithRoles() {
        var doctor = userRepository.findByEmailIgnoreCase(DOCTOR.toUpperCase()).orElseThrow();
        assertThat(doctor.getRoles()).extracting("name").extracting(Object::toString).containsExactly("DOCTOR");
        assertThat(patientRepository.count()).isGreaterThanOrEqualTo(40);
    }

    @Test
    void searchMatchesNameAndMrnCaseInsensitively() {
        newPatient("IT-SEARCH-1");
        var byName = patientRepository.search("%quarter%", PageRequest.of(0, 10));
        var byMrn = patientRepository.search("%it-search%", PageRequest.of(0, 10));
        var byFullName = patientRepository.search("%zoya quarterman%", PageRequest.of(0, 10));
        assertThat(byName.getContent()).extracting(Patient::getMrn).contains("IT-SEARCH-1");
        assertThat(byMrn.getTotalElements()).isEqualTo(1);
        assertThat(byFullName.getTotalElements()).isEqualTo(1);
    }

    @Test
    void jsonColumnsRoundTrip() {
        Patient patient = newPatient("IT-JSON-1");
        Assessment a = new Assessment();
        a.setPatient(patient);
        a.setFeatures(TestData.features());
        a.setProbability(new BigDecimal("0.6612"));
        a.setRiskBand(RiskBand.MODERATE);
        a.setPrediction("malignant");
        a.setModelName("logistic_regression");
        a.setModelVersion("1.0.0");
        a.setTopFeatures(List.of(new FeatureContribution("worst_area", 1001.0, 0.42, "increases_risk")));
        Long id = assessmentRepository.saveAndFlush(a).getId();
        entityManager.clear();

        Assessment loaded = assessmentRepository.findWithPatientById(id).orElseThrow();
        assertThat(loaded.getFeatures()).hasSize(30).containsEntry("mean_radius", 1.25);
        assertThat(loaded.getTopFeatures()).singleElement()
                .satisfies(f -> assertThat(f.getShapValue()).isEqualTo(0.42));
        assertThat(loaded.getPatient().getMrn()).isEqualTo("IT-JSON-1");
    }

    @Test
    void historyIsScopedToPatientAndSorted() {
        Patient patient = patientRepository.search("%mrn-230001%", PageRequest.of(0, 1)).getContent().get(0);
        var page = assessmentRepository.findByPatientId(patient.getId(),
                PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "createdAt")));
        assertThat(page.getContent()).isNotEmpty()
                .allSatisfy(a -> assertThat(a.getPatient().getId()).isEqualTo(patient.getId()));
        assertThat(page.getContent()).extracting(Assessment::getCreatedAt)
                .isSortedAccordingTo((x, y) -> y.compareTo(x));
    }

    @Test
    void aggregateQueriesCoverSeedData() {
        long total = assessmentRepository.count();
        long sumByBand = assessmentRepository.countByRiskBand().stream().mapToLong(c -> c.getTotal()).sum();
        assertThat(sumByBand).isEqualTo(total);

        var monthly = assessmentRepository.monthlyTrend();
        assertThat(monthly).extracting(m -> m.getMonth()).contains("2023-01", "2023-07");
        assertThat(monthly).allSatisfy(m ->
                assertThat(m.getLow() + m.getModerate() + m.getHigh()).isEqualTo(m.getTotal()));
        assertThat(assessmentRepository.countPatientsWithLatestBand("HIGH")).isPositive();
    }

    @Test
    void exportQueryFiltersByRangeAndPatient() {
        var all = assessmentRepository.findForExport(Instant.parse("2023-01-01T00:00:00Z"),
                Instant.parse("2023-08-01T00:00:00Z"), null);
        var march = assessmentRepository.findForExport(Instant.parse("2023-03-01T00:00:00Z"),
                Instant.parse("2023-04-01T00:00:00Z"), null);
        assertThat(all.size()).isGreaterThan(march.size());
        assertThat(march).allSatisfy(a -> assertThat(a.getCreatedAt())
                .isBetween(Instant.parse("2023-03-01T00:00:00Z"), Instant.parse("2023-04-01T00:00:00Z")));

        Long patientId = all.get(0).getPatient().getId();
        assertThat(assessmentRepository.findForExport(Instant.EPOCH, Instant.parse("2030-01-01T00:00:00Z"), patientId))
                .allSatisfy(a -> assertThat(a.getPatient().getId()).isEqualTo(patientId));
    }
}
