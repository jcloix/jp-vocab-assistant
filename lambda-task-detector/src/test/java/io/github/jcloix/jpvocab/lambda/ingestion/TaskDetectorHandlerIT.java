package io.github.jcloix.jpvocab.lambda.ingestion;

import com.google.api.services.docs.v1.Docs;
import io.github.jcloix.jpvocab.task.source.GoogleDocsAuth;
import io.github.jcloix.jpvocab.domain.NormalizedWord;
import io.github.jcloix.jpvocab.domain.WordNormalizer;
import io.github.jcloix.jpvocab.domain.VocabTask;
import io.github.jcloix.jpvocab.notification.DiscordNotifier;
import io.github.jcloix.jpvocab.task.source.TaskDetectionService;
import io.github.jcloix.jpvocab.task.source.VocabTaskParser;
import io.github.jcloix.jpvocab.ai.GeminiTextGenerator;
import io.github.jcloix.jpvocab.ai.InMemoryQuotaManager;
import io.github.jcloix.jpvocab.ai.TextGenerator;
import io.github.jcloix.jpvocab.task.source.GoogleDocsTaskSource;
import io.github.jcloix.jpvocab.util.LambdaLogger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

@Tag("integration")
@EnabledIfEnvironmentVariable(named = "GOOGLE_DOC_ID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
class TaskDetectorHandlerIT {

    @Test
    void shouldDetectPendingTasksAndGenerateChoices() throws Exception {

        String documentId = requireEnv("GOOGLE_DOC_ID");
        String myName = "ジュリアン"; // Issue with plugin EnvFile in local.
        String geminiApiKey = requireEnv("GEMINI_API_KEY");

        // 1. Fetch tasks
        LambdaLogger.log("📄 Fetching tasks from Google Docs...");
        Docs docsClient = GoogleDocsAuth.getDocsServiceWithOAuth();
        GoogleDocsTaskSource source = new GoogleDocsTaskSource(docsClient, documentId, new VocabTaskParser());

        List<VocabTask> allTasks = source.fetchTasks();
        LambdaLogger.log("✓ Found " + allTasks.size() + " total tasks\n");

        assertFalse(allTasks.isEmpty(), "Document should contain at least one task");

        // 2. Detect pending tasks
        LambdaLogger.log("🔍 Detecting pending tasks...");
        TaskDetectionService detector = new TaskDetectionService(myName);
        List<VocabTask> pending = detector.detectPendingTasks(allTasks);
        LambdaLogger.log("✓ Found " + pending.size() + " pending tasks\n");

        if (pending.isEmpty()) {
            LambdaLogger.log("🎉 No pending tasks!");
            return;
        }

        // 3. Normalize words
        List<NormalizedWord> normalizedWords = new ArrayList<>();
        for (VocabTask task : pending) {
            normalizedWords.add(WordNormalizer.normalize(task.getWord()));
        }

        // 4. Batch Gemini call
        LambdaLogger.log("🤖 Generating example sentences with Gemini (batch)…\n");
        TextGenerator geminiGenerator = new GeminiTextGenerator(geminiApiKey, new InMemoryQuotaManager());

        List<List<String>> results;
        try {
            results = geminiGenerator.generateExampleChoices(normalizedWords);
        } finally {
            geminiGenerator.close();
        }

        // 5. Send to Discord / print results
        for (int i = 0; i < pending.size(); i++) {
            VocabTask task = pending.get(i);
            List<String> choices = results.get(i);

            LambdaLogger.log("Word: " + task.getWord());

            if (choices == null || choices.isEmpty()) {
                LambdaLogger.log("⚠ No choices generated");
                continue;
            }

            for (String choice : choices) {
                LambdaLogger.log("Choice: " + choice);
            }

            DiscordNotifier.sendTaskMessage(task.getWord(), task.getRawWord(), task.getRowId(), choices);
            LambdaLogger.log("---");
        }

        LambdaLogger.log("\n✓ Task detection workflow complete!");
    }

    private String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required env var: " + name);
        }
        return value;
    }
}
