package com.cancerrisk.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.cancerrisk.domain.Assessment;
import com.cancerrisk.domain.FeatureContribution;
import com.cancerrisk.domain.RiskBand;
import com.cancerrisk.support.TestData;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class CsvExporterTest {

    @Test
    void writesHeaderAndOneRowPerAssessment() {
        Assessment a = new Assessment();
        a.setId(4L);
        a.setPatient(TestData.patient(1L, "MRN-1"));
        a.getPatient().setLastName("D'Souza, Jr");
        a.setFeatures(TestData.features());
        a.setProbability(new BigDecimal("0.8123"));
        a.setRiskBand(RiskBand.HIGH);
        a.setPrediction("malignant");
        a.setModelName("logistic_regression");
        a.setModelVersion("1.0.0");
        a.setCreatedBy("doc@example.com");
        a.setCreatedAt(Instant.parse("2023-05-14T09:12:44Z"));
        a.setTopFeatures(List.of(new FeatureContribution("worst_radius", 21.0, 0.7, "increases_risk"),
                new FeatureContribution("mean_area", 900.0, 0.2, "increases_risk")));

        String[] lines = new CsvExporter().export(List.of(a)).split("\r\n");

        assertThat(lines).hasSize(2);
        assertThat(lines[0]).startsWith("assessment_id,created_at,patient_mrn").endsWith("worst_fractal_dimension");
        assertThat(lines[1]).contains("\"Meera D'Souza, Jr\"").contains("worst_radius|mean_area").contains("0.8123");
        assertThat(lines[0].split(",")).hasSize(41);
    }

    @Test
    void escapesFormulaInjectionButNotNegativeNumbers() {
        assertThat(CsvExporter.escape("=HYPERLINK(1)")).isEqualTo("'=HYPERLINK(1)");
        assertThat(CsvExporter.escape("-0.25")).isEqualTo("-0.25");
        assertThat(CsvExporter.escape("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
    }
}
