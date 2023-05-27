package com.cancerrisk.controller;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cancerrisk.exception.MlServiceException;
import com.cancerrisk.repository.PatientRepository;
import com.cancerrisk.support.AbstractIntegrationTest;
import com.cancerrisk.support.TestData;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class AssessmentControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PatientRepository patientRepository;

    private long seededPatientId() {
        return patientRepository.search("%mrn-230002%", PageRequest.of(0, 1)).getContent().get(0).getId();
    }

    private String requestBody(long patientId) throws Exception {
        return objectMapper.writeValueAsString(Map.of("patientId", patientId, "features", TestData.features()));
    }

    @Test
    void doctorCreatesAssessmentThatShowsInHistory() throws Exception {
        when(mlServiceClient.predict(anyMap(), anyString())).thenReturn(TestData.prediction(0.8731, "HIGH"));
        long patientId = seededPatientId();
        String token = bearer(DOCTOR);

        String body = mockMvc.perform(post("/api/assessments").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody(patientId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.riskBand").value("HIGH"))
                .andExpect(jsonPath("$.probability").value(0.8731))
                .andExpect(jsonPath("$.createdBy").value(DOCTOR))
                .andExpect(jsonPath("$.topFeatures[0].feature").value("worst_radius"))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(get("/api/assessments/" + id).header(HttpHeaders.AUTHORIZATION, bearer(ANALYST)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.features.mean_radius").value(1.25));

        mockMvc.perform(get("/api/patients/" + patientId + "/assessments").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id));
    }

    @Test
    void analystCannotCreateAssessments() throws Exception {
        mockMvc.perform(post("/api/assessments").header(HttpHeaders.AUTHORIZATION, bearer(ANALYST))
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody(seededPatientId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void mlOutageReturnsServiceUnavailable() throws Exception {
        when(mlServiceClient.predict(anyMap(), anyString())).thenThrow(MlServiceException.unavailable());
        mockMvc.perform(post("/api/assessments").header(HttpHeaders.AUTHORIZATION, bearer(DOCTOR))
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody(seededPatientId())))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(startsWith("Prediction service")));
    }

    @Test
    void unknownPatientIsNotFound() throws Exception {
        mockMvc.perform(post("/api/assessments").header(HttpHeaders.AUTHORIZATION, bearer(DOCTOR))
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody(987654)))
                .andExpect(status().isNotFound());
    }

    @Test
    void listFiltersByRiskBand() throws Exception {
        mockMvc.perform(get("/api/assessments").param("riskBand", "HIGH").param("size", "100")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ANALYST)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.riskBand != 'HIGH')]").isEmpty());
    }

    @Test
    void exportsCsv() throws Exception {
        mockMvc.perform(get("/api/assessments/export").param("from", "2023-01-01").param("to", "2023-07-31")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ANALYST)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"assessments.csv\""))
                .andExpect(content().string(startsWith("assessment_id,created_at,patient_mrn")));
    }

    @Test
    void statisticsEndpointsRespond() throws Exception {
        String token = bearer(ANALYST);
        mockMvc.perform(get("/api/stats/summary").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assessmentsByRiskBand.HIGH").isNumber());
        mockMvc.perform(get("/api/stats/monthly").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].month").value("2023-01"));
        when(mlServiceClient.modelInfo()).thenThrow(MlServiceException.unavailable());
        mockMvc.perform(get("/api/stats/model").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }
}
