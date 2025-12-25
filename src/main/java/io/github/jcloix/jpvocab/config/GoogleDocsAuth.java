package io.github.jcloix.jpvocab.config;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.DocsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;

/**
 * GoogleDocsAuth is responsible for creating an authenticated Google Docs API client.
 *
 * Supports two authentication modes:
 * 1. Service Account (for automated Lambda execution)
 * 2. OAuth 2.0 (for personal account access, useful for local testing)
 */
public class GoogleDocsAuth {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    /**
     * Authenticate using a service account (original method)
     */
    public static Docs getDocsService(String credentialsPath) throws GeneralSecurityException, IOException {
        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(credentialsPath))
                .createScoped(Collections.singleton(DocsScopes.DOCUMENTS_READONLY));

        return new Docs.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                new HttpCredentialsAdapter(credentials)
        ).setApplicationName("JPVocab Assistant")
                .build();
    }

    /**
     * Authenticate using OAuth 2.0 with your personal Google account.
     *
     * First run: Opens browser for you to authorize the app
     * Subsequent runs: Uses stored refresh token
     *
     * @param clientSecretPath Path to client_secret.json from Google Cloud Console
     * @param tokensDirectory Directory where tokens will be stored (e.g., "tokens")
     */
    public static Docs getDocsServiceWithOAuth(String clientSecretPath, String tokensDirectory)
            throws GeneralSecurityException, IOException {

        // Load client secrets
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                JSON_FACTORY,
                new InputStreamReader(new FileInputStream(clientSecretPath))
        );

        // Build flow and trigger user authorization
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                clientSecrets,
                Collections.singleton(DocsScopes.DOCUMENTS_READONLY)
        )
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(tokensDirectory)))
                .setAccessType("offline")
                .build();

        // Get credentials (will open browser on first run)
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

        // Build and return Docs service
        return new Docs.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                credential
        ).setApplicationName("JPVocab Assistant")
                .build();
    }
}