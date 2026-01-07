package io.github.jcloix.jpvocab.task.source;

import io.github.jcloix.jpvocab.util.LambdaLogger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@Tag("integration")
@EnabledIfEnvironmentVariable(named = "GOOGLE_CLIENT_ID", matches = ".+")
class WebOAuthTokenGeneratorIT {

    @Test
    void shouldPrintWebOAuthRefreshToken() throws Exception {

        String clientId = requireEnv("GOOGLE_CLIENT_ID");
        String clientSecret = requireEnv("GOOGLE_CLIENT_SECRET");

        LambdaLogger.log("Generating Web OAuth refresh token...");
        WebOAuthTokenGenerator.generateRefreshToken(clientId, clientSecret);
    }

    private String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required env var: " + name);
        }
        return value;
    }
}
