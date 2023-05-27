package com.cancerrisk.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cancerrisk.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void loginReturnsTokensAndProfile() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + DOCTOR + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.roles[0]").value("DOCTOR"));
    }

    @Test
    void wrongPasswordIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + DOCTOR + "\",\"password\":\"nope12345\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void registerValidatesInput() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"\",\"email\":\"bad\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                // blank name, invalid email, and the password is both too short and missing a digit
                .andExpect(jsonPath("$.details.length()").value(4));
    }

    @Test
    void registerThenUseAccessAndRefreshTokens() throws Exception {
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Harini Rao\",\"email\":\"harini.it@example.com\","
                                + "\"password\":\"secret123\",\"role\":\"DOCTOR\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        var json = objectMapper.readTree(body);

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + json.get("accessToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("harini.it@example.com"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + json.get("refreshToken").asText() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        // a refresh token must not work as an access token
        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + json.get("refreshToken").asText()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateEmailIsConflict() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Dup\",\"email\":\"" + DOCTOR + "\",\"password\":\"secret123\"}"))
                .andExpect(status().isConflict());
    }
}
