package io.github.jcloix.jpvocab.local;

import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.FileDataStoreFactory;
import io.github.jcloix.jpvocab.util.LambdaLogger;

import java.io.File;

public class ExtractRefreshToken {
    public static void main(String[] args) throws Exception {
        String tokensDirectory = "tokens"; // same as LocalOAuthGenerator
        FileDataStoreFactory dataStoreFactory = new FileDataStoreFactory(new File(tokensDirectory));

        // Use raw type to bypass generic bounds
        DataStore rawDataStore = dataStoreFactory.getDataStore("StoredCredential");

        // The actual object is StoredCredential
        StoredCredential stored = (StoredCredential) rawDataStore.get("user"); // key = "user"

        if (stored != null && stored.getRefreshToken() != null) {
            LambdaLogger.log("REFRESH_TOKEN = " + stored.getRefreshToken());
        } else {
            LambdaLogger.log("Refresh token not found. Run OAuth flow first.");
        }
    }
}
