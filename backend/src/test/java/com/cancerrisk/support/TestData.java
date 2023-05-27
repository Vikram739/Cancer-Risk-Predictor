package com.cancerrisk.support;

import com.cancerrisk.client.MlPrediction;
import com.cancerrisk.domain.Patient;
import com.cancerrisk.domain.Sex;
import com.cancerrisk.service.FeatureCatalog;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TestData {

    private TestData() {
    }

    public static Map<String, Double> features() {
        Map<String, Double> features = new LinkedHashMap<>();
        FeatureCatalog.FEATURES.forEach(f -> features.put(f, 1.25));
        return features;
    }

    public static MlPrediction prediction(double probability, String band) {
        return new MlPrediction("MRN-1", probability, probability >= 0.5 ? "malignant" : "benign", band,
                "logistic_regression", "1.0.0",
                List.of(new MlPrediction.TopFeature("worst_radius", 25.4, 0.81, "increases_risk"),
                        new MlPrediction.TopFeature("mean_texture", 17.2, -0.12, "decreases_risk")));
    }

    public static Patient patient(Long id, String mrn) {
        Patient p = new Patient();
        p.setId(id);
        p.setMrn(mrn);
        p.setFirstName("Meera");
        p.setLastName("Pillai");
        p.setDateOfBirth(LocalDate.of(1972, 4, 9));
        p.setSex(Sex.FEMALE);
        return p;
    }
}
