package io.github.jcloix.jpvocab;


import com.google.api.services.docs.v1.Docs;
import io.github.jcloix.jpvocab.config.GoogleDocsAuth;
import io.github.jcloix.jpvocab.model.VocabTask;
import io.github.jcloix.jpvocab.service.GeminiService;
import io.github.jcloix.jpvocab.service.TaskDetectionService;
import io.github.jcloix.jpvocab.service.VocabTaskParser;
import io.github.jcloix.jpvocab.service.source.GoogleDocsTaskSource;

import java.util.List;

public class LocalEndToEndTest {
    public static void main(String[] args) throws Exception {
        // Configuration
        String clientSecretPath = System.getenv("GOOGLE_CREDENTIALS_PATH");
        String documentId = System.getenv("GOOGLE_DOC_ID");
        String tokensDirectory = System.getenv("TOKEN_FOLDER");
        String myName = System.getenv("MY_NAME");
        String geminiApiKey = System.getenv("GEMINI_API_KEY");

        // 1. Fetch tasks from Google Docs
        System.out.println("📄 Fetching tasks from Google Docs...");
        Docs docsClient = GoogleDocsAuth.getDocsServiceWithOAuth(clientSecretPath, tokensDirectory);
        VocabTaskParser parser = new VocabTaskParser();
        GoogleDocsTaskSource source = new GoogleDocsTaskSource(docsClient, documentId, parser);
        List<VocabTask> allTasks = source.fetchTasks();
        System.out.println("✓ Found " + allTasks.size() + " total tasks\n");

        // 2. Detect pending tasks
        System.out.println("🔍 Detecting pending tasks...");
        TaskDetectionService detector = new TaskDetectionService(myName);
        List<VocabTask> pending = detector.detectPendingTasks(allTasks);
        System.out.println("✓ Found " + pending.size() + " pending tasks\n");

        if (pending.isEmpty()) {
            System.out.println("🎉 No pending tasks! You're all caught up!");
            return;
        }

        // 3. Generate example sentences with Gemini
        System.out.println("🤖 Generating example sentences with Gemini...\n");
        GeminiService geminiService = new GeminiService(geminiApiKey, "gemini-flash-latest");

        for (VocabTask task : pending) {
            System.out.println("Word: " + task.getWord());
            try {
                String sentence = geminiService.generateExampleSentence(task.getWord());
                System.out.println("Generated: " + sentence);
                System.out.println("---");
            } catch (Exception e) {
                System.err.println("❌ Error: " + e.getMessage());
            }
        }

        geminiService.close();
        System.out.println("\n✓ Complete!");
    }
}
