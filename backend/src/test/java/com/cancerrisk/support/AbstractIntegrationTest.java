package com.cancerrisk.support;

import com.cancerrisk.client.MlServiceClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Full application context against PostgreSQL, every test rolls back. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ExtendWith(DatabaseAvailableCondition.class)
public abstract class AbstractIntegrationTest {

    protected static final String PASSWORD = "Password@123";
    protected static final String DOCTOR = "rohan.kapoor@cancerrisk.dev";
    protected static final String ANALYST = "kabir.shah@cancerrisk.dev";
    protected static final String ADMIN = "admin@cancerrisk.dev";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockBean
    protected MlServiceClient mlServiceClient;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        IntegrationDatabase.register(registry);
    }

    protected String bearer(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(body).get("accessToken").asText();
    }
}
