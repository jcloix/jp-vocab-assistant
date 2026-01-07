package io.github.jcloix.jpvocab.task.source;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.docs.v1.DocsScopes;

import java.util.Collections;

public class WebOAuthTokenGenerator {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    public static void generateRefreshToken(
            String clientId,
            String clientSecret
    ) throws Exception {

        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        GoogleNetHttpTransport.newTrustedTransport(),
                        JSON_FACTORY,
                        clientId,
                        clientSecret,
                        Collections.singleton(DocsScopes.DOCUMENTS)
                )
                        .setAccessType("offline")
                        .setApprovalPrompt("force") // IMPORTANT
                        .build();

        LocalServerReceiver receiver =
                new LocalServerReceiver.Builder().setPort(8888).build();

        Credential credential =
                new AuthorizationCodeInstalledApp(flow, receiver)
                        .authorize("user");

        System.out.println("REFRESH TOKEN:");
        System.out.println(credential.getRefreshToken());
    }
}

