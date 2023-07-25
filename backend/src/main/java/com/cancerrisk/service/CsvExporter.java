package com.cancerrisk.service;

import com.cancerrisk.domain.Assessment;
import com.cancerrisk.domain.FeatureContribution;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class CsvExporter {

    private static final List<String> BASE_COLUMNS = List.of(
            "assessment_id", "created_at", "patient_mrn", "patient_name", "probability", "risk_band",
            "prediction", "model_name", "model_version", "created_by", "top_features");

    public String export(List<Assessment> assessments) {
        List<String> header = new ArrayList<>(BASE_COLUMNS);
        header.addAll(FeatureCatalog.FEATURES);

        StringBuilder sb = new StringBuilder();
        appendRow(sb, header);
        for (Assessment a : assessments) {
            List<String> row = new ArrayList<>(header.size());
            row.add(String.valueOf(a.getId()));
            row.add(String.valueOf(a.getCreatedAt()));
            row.add(a.getPatient().getMrn());
            row.add(a.getPatient().getFullName());
            row.add(a.getProbability().toPlainString());
            row.add(a.getRiskBand().name());
            row.add(a.getPrediction());
            row.add(a.getModelName());
            row.add(a.getModelVersion());
            row.add(a.getCreatedBy());
            row.add(a.getTopFeatures().stream().map(FeatureContribution::getFeature).collect(Collectors.joining("|")));
            for (String feature : FeatureCatalog.FEATURES) {
                Double value = a.getFeatures().get(feature);
                row.add(value == null ? "" : value.toString());
            }
            appendRow(sb, row);
        }
        return sb.toString();
    }

    private static void appendRow(StringBuilder sb, List<String> values) {
        sb.append(values.stream().map(CsvExporter::escape).collect(Collectors.joining(","))).append("\r\n");
    }

    static String escape(String value) {
        if (value == null) {
            return "";
        }
        // guard against spreadsheet formula injection
        if (!value.isEmpty() && "=+-@".indexOf(value.charAt(0)) >= 0 && !isNumeric(value)) {
            value = "'" + value;
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static boolean isNumeric(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}
