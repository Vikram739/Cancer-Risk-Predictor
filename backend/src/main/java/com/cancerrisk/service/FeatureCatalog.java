package com.cancerrisk.service;

import java.util.List;

/** The 30 Wisconsin Diagnostic features in the order the ML service was trained on. */
public final class FeatureCatalog {

    private static final List<String> MEASUREMENTS = List.of(
            "radius", "texture", "perimeter", "area", "smoothness",
            "compactness", "concavity", "concave_points", "symmetry", "fractal_dimension");

    public static final List<String> FEATURES = build();

    private FeatureCatalog() {
    }

    private static List<String> build() {
        var mean = MEASUREMENTS.stream().map(m -> "mean_" + m);
        var error = MEASUREMENTS.stream().map(m -> m + "_error");
        var worst = MEASUREMENTS.stream().map(m -> "worst_" + m);
        return java.util.stream.Stream.of(mean, error, worst).flatMap(s -> s).toList();
    }
}
