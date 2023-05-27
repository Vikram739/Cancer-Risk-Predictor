package com.cancerrisk.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.cancerrisk.exception.MlServiceException;
import com.cancerrisk.support.TestData;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class MlServiceClientTest {

    private static final String PREDICTION = """
            {"reference":"MRN-1","probability":0.8731,"prediction":"malignant","risk_band":"HIGH",
             "model_name":"xgboost","model_version":"1.0.0",
             "top_features":[{"feature":"worst_radius","value":24.1,"shap_value":1.21,"direction":"increases_risk"}]}
            """;

    private MockRestServiceServer server;
    private MlServiceClient client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplateBuilder().rootUri("http://ml").build();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        client = new MlServiceClient(restTemplate,
                new MlClientProperties("http://ml", Duration.ofSeconds(1), Duration.ofSeconds(1), 3, Duration.ofMillis(1)));
    }

    @Test
    void parsesPrediction() {
        server.expect(once(), requestTo("http://ml/predict"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.reference").value("MRN-1"))
                .andExpect(jsonPath("$.features.mean_radius").value(1.25))
                .andRespond(withSuccess(PREDICTION, MediaType.APPLICATION_JSON));

        MlPrediction prediction = client.predict(TestData.features(), "MRN-1");

        assertThat(prediction.riskBand()).isEqualTo("HIGH");
        assertThat(prediction.topFeatures()).singleElement()
                .satisfies(f -> assertThat(f.shapValue()).isEqualTo(1.21));
        server.verify();
    }

    @Test
    void retriesServerErrorsThenSucceeds() {
        server.expect(times(2), requestTo("http://ml/predict")).andRespond(withServerError());
        server.expect(once(), requestTo("http://ml/predict"))
                .andRespond(withSuccess(PREDICTION, MediaType.APPLICATION_JSON));

        assertThat(client.predict(TestData.features(), null).probability()).isEqualTo(0.8731);
        server.verify();
    }

    @Test
    void givesUpAfterMaxAttempts() {
        server.expect(times(3), requestTo("http://ml/predict")).andRespond(withServerError());

        assertThatThrownBy(() -> client.predict(TestData.features(), null))
                .isInstanceOf(MlServiceException.class)
                .satisfies(ex -> assertThat(((MlServiceException) ex).getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
        server.verify();
    }

    @Test
    void doesNotRetryClientErrors() {
        server.expect(once(), requestTo("http://ml/predict")).andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY));

        assertThatThrownBy(() -> client.predict(TestData.features(), null))
                .isInstanceOf(MlServiceException.class)
                .satisfies(ex -> assertThat(((MlServiceException) ex).getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        server.verify();
    }

    @Test
    void modelInfoFallsBackToCachedResponse() {
        server.expect(once(), requestTo("http://ml/model/info"))
                .andRespond(withSuccess("{\"model_name\":\"xgboost\"}", MediaType.APPLICATION_JSON));
        server.expect(times(3), requestTo("http://ml/model/info")).andRespond(withServerError());

        assertThat(client.modelInfo()).containsEntry("model_name", "xgboost");
        assertThat(client.modelInfo()).containsEntry("model_name", "xgboost");
        server.verify();
    }
}
