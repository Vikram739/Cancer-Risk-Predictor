package com.cancerrisk.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Response body of POST /predict on the ML service. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MlPrediction(
        String reference,
        double probability,
        String prediction,
        @JsonProperty("risk_band") String riskBand,
        @JsonProperty("model_name") String modelName,
        @JsonProperty("model_version") String modelVersion,
        @JsonProperty("top_features") List<TopFeature> topFeatures) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TopFeature(
            String feature,
            double value,
            @JsonProperty("shap_value") double shapValue,
            String direction) {
    }
}
