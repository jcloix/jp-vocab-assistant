package io.github.jcloix.jpvocab.task.source;

import com.google.api.services.docs.v1.Docs;
import io.github.jcloix.jpvocab.util.LambdaLogger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@Tag("integration")
@EnabledIfEnvironmentVariable(named = "GOOGLE_REFRESH_TOKEN", matches = ".+")
class GoogleDocsWriteIT {

    @Test
    void shouldWriteAndRestoreSentenceForWord() throws Exception {

        // Arrange
        String documentId = requireEnv("GOOGLE_DOC_ID");
        String testWord = "価値";
        int testRowId = 3;

        Docs docsClient = GoogleDocsAuth.getDocsServiceWithOAuth();
        GoogleDocsTaskSource taskSource = new GoogleDocsTaskSource(
                docsClient,
                documentId,
                new VocabTaskParser()
        );

        // Current sentence in the doc
        String originalSentence = "例）一度食べてみる価値があるのではないだろうか。";
        LambdaLogger.log("Original sentence: " + originalSentence);

        // Test sentence
        String testSentence = "これはテスト文です " + System.currentTimeMillis();

        try {
            // Act: write test sentence
            taskSource.completeTask(testWord, testRowId, testSentence);
            LambdaLogger.log("✓ Wrote test sentence: " + testSentence);

            // Optional: fetch again to verify (if fetchTasks() provides sentence)
            // VocabTask updatedTask = taskSource.fetchTasks().stream()
            //        .filter(t -> t.getWord().equals(testWord) && t.getRowId() == testRowId)
            //        .findFirst().orElseThrow();
            // assertEquals(testSentence, updatedTask.getSentence());

        } finally {
            // Restore original sentence
            taskSource.completeTask(testWord, testRowId, originalSentence);
            LambdaLogger.log("✓ Restored original sentence: " + originalSentence);
        }
    }

    private String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required env var: " + name);
        }
        return value;
    }
}
