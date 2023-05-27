package com.cancerrisk.support;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.DockerClientFactory;

/**
 * Integration tests need PostgreSQL: either an external database given by
 * TEST_DATABASE_URL (handy in CI with a service container) or Docker for Testcontainers.
 */
public class DatabaseAvailableCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (IntegrationDatabase.externalUrl() != null) {
            return ConditionEvaluationResult.enabled("using TEST_DATABASE_URL");
        }
        try {
            if (DockerClientFactory.instance().isDockerAvailable()) {
                return ConditionEvaluationResult.enabled("Docker available for Testcontainers");
            }
        } catch (Exception ignored) {
            // fall through
        }
        return ConditionEvaluationResult.disabled("no Docker and no TEST_DATABASE_URL, skipping integration test");
    }
}
