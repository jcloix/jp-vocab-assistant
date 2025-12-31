package io.github.jcloix.jpvocab.task.source;

import com.google.api.services.docs.v1.Docs;
import io.github.jcloix.jpvocab.domain.VocabTask;
import io.github.jcloix.jpvocab.util.LambdaLogger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

@Tag("integration")
@EnabledIfEnvironmentVariable(named = "GOOGLE_CREDENTIALS_PATH", matches = ".+")
class LocalOAuthGeneratorIT {

    @Test
    void shouldPrintRefreshToken() throws Exception {
        String clientSecretPath = requireEnv("GOOGLE_CREDENTIALS_PATH");
        String tokensDirectory = requireEnv("TOKEN_FOLDER");

        LambdaLogger.log("Authenticating with Google (OAuth)...");
        Docs docsClient =
                LocalOAuthGenerator.getDocsServiceWithOAuth(clientSecretPath, tokensDirectory);

    }

    private String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required env var: " + name);
        }
        return value;
    }
}
