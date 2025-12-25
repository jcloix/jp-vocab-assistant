package io.github.jcloix.jpvocab.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.api.services.docs.v1.Docs;
import io.github.jcloix.jpvocab.config.GoogleDocsAuth;
import io.github.jcloix.jpvocab.model.VocabTask;
import io.github.jcloix.jpvocab.service.GeminiService;
import io.github.jcloix.jpvocab.service.TaskDetectionService;
import io.github.jcloix.jpvocab.service.VocabTaskParser;
import io.github.jcloix.jpvocab.service.source.GoogleDocsTaskSource;
import io.github.jcloix.jpvocab.service.source.TaskSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskDetectorHandler implements RequestHandler<Void, Map<String, Object>> {

    @Override
    public Map<String, Object> handleRequest(Void input, Context context) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Get configuration from environment variables
            String credentialsPath = System.getenv("GOOGLE_CREDENTIALS_PATH");
            String documentId = System.getenv("GOOGLE_DOC_ID");
            String token = System.getenv("TOKEN_FOLDER");
            String myName = System.getenv("MY_NAME");
            String geminiApiKey = System.getenv("GEMINI_API_KEY");

            context.getLogger().log("Starting task detection for user: " + myName);

            // Initialize Google Docs client
            Docs docsClient = GoogleDocsAuth.getDocsServiceWithOAuth(credentialsPath,token);

            // Fetch all tasks
            VocabTaskParser parser = new VocabTaskParser();
            TaskSource source = new GoogleDocsTaskSource(docsClient, documentId, parser);
            List<VocabTask> allTasks = source.fetchTasks();
            context.getLogger().log("Fetched " + allTasks.size() + " total tasks");

            // Detect pending tasks
            TaskDetectionService detector = new TaskDetectionService(myName);
            List<VocabTask> pending = detector.detectPendingTasks(allTasks);
            context.getLogger().log("Found " + pending.size() + " pending tasks");

            response.put("totalTasks", allTasks.size());
            response.put("pendingTasks", pending.size());

            // Generate sentences for pending tasks
            if (!pending.isEmpty() && geminiApiKey != null && !geminiApiKey.isEmpty()) {
                context.getLogger().log("Generating example sentences with Gemini...");
                GeminiService geminiService = new GeminiService(geminiApiKey, "gemini-2.0-flash");

                Map<String, String> generatedSentences = new HashMap<>();

                for (VocabTask task : pending) {
                    try {
                        context.getLogger().log("Processing word: " + task.getWord());
                        String sentence = geminiService.generateExampleSentence(task.getWord());
                        generatedSentences.put(task.getWord(), sentence);
                        context.getLogger().log("Generated: " + sentence);
                    } catch (Exception e) {
                        context.getLogger().log("Error generating sentence for " + task.getWord() + ": " + e.getMessage());
                    }
                }

                geminiService.close();
                response.put("generatedSentences", generatedSentences);
            }

            response.put("status", "success");
            return response;

        } catch (Exception e) {
            context.getLogger().log("ERROR: " + e.getMessage());
            e.printStackTrace();
            response.put("status", "error");
            response.put("error", e.getMessage());
            return response;
        }
    }
}