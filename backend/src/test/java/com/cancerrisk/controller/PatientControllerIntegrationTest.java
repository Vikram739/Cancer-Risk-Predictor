package com.cancerrisk.controller;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cancerrisk.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class PatientControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String NEW_PATIENT = """
            {"mrn":"it-0001","firstName":"Lakshmi","lastName":"Venkat","dateOfBirth":"1966-11-02",
             "sex":"FEMALE","phone":"+91 98450 12345","email":"lakshmi@example.com"}
            """;

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void listsWithPagingAndSorting() throws Exception {
        mockMvc.perform(get("/api/patients").param("size", "5").param("sort", "dateOfBirth,desc")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ANALYST)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(40)))
                .andExpect(jsonPath("$.size").value(5));
    }

    @Test
    void rejectsUnknownSortProperty() throws Exception {
        mockMvc.perform(get("/api/patients").param("sort", "password")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ANALYST)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void doctorCanCreateUpdateButNotDelete() throws Exception {
        String token = bearer(DOCTOR);
        String body = mockMvc.perform(post("/api/patients").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_PATIENT))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mrn").value("IT-0001"))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(put("/api/patients/" + id).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NEW_PATIENT.replace("Venkat", "Venkatesh")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Lakshmi Venkatesh"));

        mockMvc.perform(get("/api/patients").param("search", "venkatesh").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(delete("/api/patients/" + id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/patients/" + id).header(HttpHeaders.AUTHORIZATION, bearer(ADMIN)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/patients/" + id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound());
    }

    @Test
    void analystCannotCreate() throws Exception {
        mockMvc.perform(post("/api/patients").header(HttpHeaders.AUTHORIZATION, bearer(ANALYST))
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_PATIENT))
                .andExpect(status().isForbidden());
    }

    @Test
    void validatesPayload() throws Exception {
        mockMvc.perform(post("/api/patients").header(HttpHeaders.AUTHORIZATION, bearer(DOCTOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mrn\":\"bad mrn!\",\"firstName\":\"A\",\"lastName\":\"B\","
                                + "\"dateOfBirth\":\"2999-01-01\",\"sex\":\"FEMALE\",\"email\":\"nope\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.length()").value(3));
    }

    @Test
    void duplicateMrnIsConflict() throws Exception {
        mockMvc.perform(post("/api/patients").header(HttpHeaders.AUTHORIZATION, bearer(DOCTOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NEW_PATIENT.replace("it-0001", "MRN-230001")))
                .andExpect(status().isConflict());
    }
}
