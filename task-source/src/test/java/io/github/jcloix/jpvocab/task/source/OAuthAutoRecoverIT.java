package io.github.jcloix.jpvocab.task.source;

import com.google.api.services.docs.v1.Docs;
import io.github.jcloix.jpvocab.util.LambdaLogger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@Tag("integration")
@EnabledIfEnvironmentVariable(named = "GOOGLE_DOC_ID", matches = ".+")
public class OAuthAutoRecoverIT {

    @Test
    void shouldUseRefreshToken_orRegenerateIfRevoked() throws Exception {

        String documentId = requireEnv("GOOGLE_DOC_ID");

        Docs docsClient;

        try {
            LambdaLogger.log("Trying existing refresh token...");
            docsClient = GoogleDocsAuth.getDocsServiceWithOAuth();

            // 🔹 minimal real call = token validity check
            docsClient.documents().get(documentId).execute();

            LambdaLogger.log("✓ Refresh token is valid");

        } catch (Exception e) {

            if (isInvalidGrant(e)) {
                LambdaLogger.log("⚠ Refresh token invalid, regenerating...");

                WebOAuthTokenGenerator.generateRefreshToken(
                        requireEnv("GOOGLE_CLIENT_ID"),
                        requireEnv("GOOGLE_CLIENT_SECRET")
                );

                throw new IllegalStateException(
                        "New refresh token generated. Update AWS and rerun test."
                );
            }

            throw e;
        }
    }

    private boolean isInvalidGrant(Exception e) {
        return e.getMessage() != null && e.getMessage().contains("invalid_grant");
    }

    private String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing env var: " + name);
        }
        return value;
    }
}
