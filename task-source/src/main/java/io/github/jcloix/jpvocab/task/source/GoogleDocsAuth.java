package io.github.jcloix.jpvocab.task.source;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.DocsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.UserCredentials;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

/**
 * Lambda-safe GoogleDocsAuth using modern UserCredentials
 *
 * Reads environment variables:
 * - GOOGLE_CLIENT_ID
 * - GOOGLE_CLIENT_SECRET
 * - GOOGLE_REFRESH_TOKEN
 */
public class GoogleDocsAuth {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    public static Docs getDocsServiceWithOAuth() throws GeneralSecurityException, IOException {

        String clientId = System.getenv("GOOGLE_CLIENT_ID");
        String clientSecret = System.getenv("GOOGLE_CLIENT_SECRET");
        String refreshToken = System.getenv("GOOGLE_REFRESH_TOKEN");

        if (clientId == null || clientSecret == null || refreshToken == null) {
            throw new IllegalStateException(
                    "Missing environment variables: GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET, GOOGLE_REFRESH_TOKEN");
        }

        // Create UserCredentials using refresh token
        UserCredentials credentials = UserCredentials.newBuilder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRefreshToken(refreshToken)
                .build();

        // Build Docs client
        return new Docs.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                new HttpCredentialsAdapter(credentials.createScoped(Collections.singleton(DocsScopes.DOCUMENTS_READONLY)))
        ).setApplicationName("JPVocab Assistant")
                .build();
    }
}
