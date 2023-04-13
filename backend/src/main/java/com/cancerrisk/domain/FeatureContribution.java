package com.cancerrisk.domain;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One SHAP contribution as stored in the assessments.top_features column. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeatureContribution implements Serializable {

    private String feature;
    private double value;
    private double shapValue;
    private String direction;
}
