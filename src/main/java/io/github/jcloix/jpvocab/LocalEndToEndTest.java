package io.github.jcloix.jpvocab;

import com.google.api.services.docs.v1.Docs;
import io.github.jcloix.jpvocab.config.GoogleDocsAuth;
import io.github.jcloix.jpvocab.domain.normalization.NormalizedWord;
import io.github.jcloix.jpvocab.domain.normalization.WordNormalizer;
import io.github.jcloix.jpvocab.model.VocabTask;
import io.github.jcloix.jpvocab.notification.DiscordNotifier;
import io.github.jcloix.jpvocab.service.TaskDetectionService;
import io.github.jcloix.jpvocab.service.VocabTaskParser;
import io.github.jcloix.jpvocab.service.ai.generation.GeminiTextGenerator;
import io.github.jcloix.jpvocab.service.ai.generation.InMemoryQuotaManager;
import io.github.jcloix.jpvocab.service.ai.generation.TextGenerator;
import io.github.jcloix.jpvocab.service.source.GoogleDocsTaskSource;
import io.github.jcloix.jpvocab.util.LambdaLogger;

import java.util.ArrayList;
import java.util.List;

public class LocalEndToEndTest {

    public static void main(String[] args) throws Exception {

        String documentId = System.getenv("GOOGLE_DOC_ID");
        String myName = System.getenv("MY_NAME");
        String geminiApiKey = System.getenv("GEMINI_API_KEY");

        // 1. Fetch tasks
        LambdaLogger.log("📄 Fetching tasks from Google Docs...");
        Docs docsClient = GoogleDocsAuth.getDocsServiceWithOAuth();
        VocabTaskParser parser = new VocabTaskParser();
        GoogleDocsTaskSource source =
                new GoogleDocsTaskSource(docsClient, documentId, parser);

        List<VocabTask> allTasks = source.fetchTasks();
        LambdaLogger.log("✓ Found " + allTasks.size() + " total tasks\n");

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

        // 4. Batch Gemini call using the new architecture
        LambdaLogger.log("🤖 Generating example sentences with Gemini (batch)…\n");
        TextGenerator geminiGenerator = new GeminiTextGenerator(
                geminiApiKey,
                new InMemoryQuotaManager() // avoids AWS/DynamoDB for local testing
        );

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

        LambdaLogger.log("\n✓ Complete!");
    }
}
