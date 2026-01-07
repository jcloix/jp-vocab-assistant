package io.github.jcloix.jpvocab.lambda.ingestion;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.api.services.docs.v1.Docs;
import io.github.jcloix.jpvocab.domain.NormalizedWord;
import io.github.jcloix.jpvocab.domain.VocabTask;
import io.github.jcloix.jpvocab.notification.DiscordNotifier;
import io.github.jcloix.jpvocab.persistence.PersistedTask;
import io.github.jcloix.jpvocab.persistence.TaskRepository;
import io.github.jcloix.jpvocab.task.source.*;
import io.github.jcloix.jpvocab.ai.*;

import io.github.jcloix.jpvocab.util.LambdaLogger;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TaskDetectorHandler implements RequestHandler<Void, Map<String, Object>> {

    private final AIProviderManager aiManager;

    public TaskDetectorHandler() throws Exception {

        // Initialize DynamoDB for quota tracking
        DynamoDbClient dynamoDbClient = DynamoDbClient.create();
        String aiUsageTable = System.getenv("AI_USAGE_TABLE");
        QuotaManager quotaManager = new QuotaManager(dynamoDbClient, aiUsageTable);

        // Initialize GeminiTextGenerator
        String geminiApiKey = System.getenv("GEMINI_API_KEY");
        TextGenerator geminiGenerator = new GeminiTextGenerator(geminiApiKey, quotaManager);

        // Register providers
        aiManager = new AIProviderManager();
        aiManager.registerProvider("gemini", geminiGenerator);
    }

    @Override
    public Map<String, Object> handleRequest(Void input, Context context) {
        try {
            HandlerContext ctx = initContext(context);

            List<VocabTask> allTasks = fetchAllTasks(ctx);
            List<VocabTask> pendingTasks = detectPendingTasks(allTasks, ctx);

            processPendingTasks(pendingTasks, ctx);

            return buildSuccessResponse(allTasks.size(), pendingTasks.size());

        } catch (Exception e) {
            checkOAuthException(e);
            return buildErrorResponse(context, e);
        } finally {
            aiManager.close(); // close all providers
        }
    }

    /* ============================= */
    /* ===== INITIALIZATION ======== */
    /* ============================= */

    private HandlerContext initContext(Context context) throws Exception {
        LambdaLogger.init(context);
        HandlerContext ctx = new HandlerContext();

        ctx.lambdaContext = context;
        ctx.documentId = System.getenv("GOOGLE_DOC_ID");
        ctx.myName = System.getenv("MY_NAME");

        LambdaLogger.log("Starting task detection for user: " + ctx.myName);

        ctx.docsClient = GoogleDocsAuth.getDocsServiceWithOAuth();
        ctx.taskRepository = new TaskRepository();

        return ctx;
    }

    /* ============================= */
    /* ===== GOOGLE DOCS =========== */
    /* ============================= */

    private List<VocabTask> fetchAllTasks(HandlerContext ctx) throws Exception {
        VocabTaskParser parser = new VocabTaskParser();
        TaskSource source = new GoogleDocsTaskSource(ctx.docsClient, ctx.documentId, parser);

        List<VocabTask> tasks = source.fetchTasks();
        ctx.lambdaContext.getLogger()
                .log("Fetched " + tasks.size() + " total tasks");

        return tasks;
    }

    /* ============================= */
    /* ===== TASK DETECTION ======== */
    /* ============================= */

    private List<VocabTask> detectPendingTasks(List<VocabTask> allTasks, HandlerContext ctx) {
        TaskDetectionService detector =
                new TaskDetectionService(ctx.myName);
        List<VocabTask> pending = detector.detectPendingTasks(allTasks);

        ctx.lambdaContext.getLogger()
                .log("Found " + pending.size() + " pending tasks");

        return pending;
    }

    /* ============================= */
    /* ===== PROCESSING ============ */
    /* ============================= */

    private void processPendingTasks(List<VocabTask> pendingTasks, HandlerContext ctx) throws Exception {
        List<VocabTask> newTasks = filterNewTasks(pendingTasks, ctx);

        if (newTasks.isEmpty()) {
            LambdaLogger.log("No new tasks to process");
            return;
        }

        List<NormalizedWord> normalizedWords = normalizeTasks(newTasks, ctx);

        // Use AIProviderManager instead of GeminiService
        List<List<String>> choicesBatch = aiManager.generateExampleChoices(normalizedWords);

        processResults(newTasks, choicesBatch, ctx);
    }

    /* ============================= */
    /* ===== HELPERS =============== */
    /* ============================= */

    private List<VocabTask> filterNewTasks(List<VocabTask> tasks, HandlerContext ctx) {
        return tasks.stream()
                .filter(task -> !taskAlreadyExists(task, ctx))
                .toList();
    }

    private List<NormalizedWord> normalizeTasks(List<VocabTask> tasks, HandlerContext ctx) {
        return tasks.stream()
                .map(task -> {
                    NormalizedWord nw = task.getNormalizedWord();
                    LambdaLogger.log(
                            String.format(
                                    "Normalized [row=%d, raw='%s', base='%s', reading='%s', context='%s', fixedPhrase='%s']",
                                    task.getRowId(),
                                    task.getWord(),
                                    nw.baseWord(),
                                    nw.reading(),
                                    nw.context(),
                                    nw.fixedPhrase()
                            )
                    );
                    return nw;
                })
                .toList();
    }

    private void processResults(List<VocabTask> tasks, List<List<String>> allChoices, HandlerContext ctx) throws Exception {
        if (tasks.size() != allChoices.size()) {
            throw new IllegalStateException(
                    "Mismatch between tasks and AI results: " + tasks.size() + " vs " + allChoices.size()
            );
        }

        for (int i = 0; i < tasks.size(); i++) {
            VocabTask task = tasks.get(i);
            List<String> choices = allChoices.get(i);

            persistTask(task, choices, ctx);
            notifyDiscord(task, choices, ctx);
        }
    }

    private boolean taskAlreadyExists(VocabTask task, HandlerContext ctx) {
        Optional<PersistedTask> existing = ctx.taskRepository
                .findByWord(ctx.documentId, task.getWord(), task.getRowId());
        return existing.isPresent();
    }

    private void persistTask(VocabTask task, List<String> choices, HandlerContext ctx) {
        ctx.taskRepository.saveNewTask(ctx.documentId, task.getRowId(), task.getWord(), choices);
    }

    private void notifyDiscord(VocabTask task, List<String> choices, HandlerContext ctx) throws Exception {
        DiscordNotifier.sendTaskMessage(task.getWord(), task.getRawWord(),task.getRowId(), choices);

        Optional<PersistedTask> persisted = ctx.taskRepository
                .findByWord(ctx.documentId, task.getWord(), task.getRowId());

        persisted.ifPresent(pt -> ctx.taskRepository.markNotified(ctx.documentId, pt.getRowId(), pt.getWord()));
    }

    /* ============================= */
    /* ===== RESPONSES ============= */
    /* ============================= */

    private Map<String, Object> buildSuccessResponse(int total, int pending) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("totalTasks", total);
        response.put("pendingTasks", pending);
        return response;
    }

    private Map<String, Object> buildErrorResponse(Context context, Exception e) {
        LambdaLogger.log("Error:",e);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("error", e.toString());
        return response;
    }

    private void checkOAuthException(Exception e) {
        if (isOAuthInvalidGrant(e)) {
            LambdaLogger.log("❌ GOOGLE OAUTH REFRESH TOKEN INVALID OR REVOKED");
            LambdaLogger.log("❌ Manual re-authentication required");
            try {
                DiscordNotifier.sendMessage("❌ Issue in OAuth");
            } catch (Exception ex) {
                LambdaLogger.log("Issue send Discord notification");
            }
        }
    }

    private boolean isOAuthInvalidGrant(Throwable e) {
        Throwable t = e;
        while (t != null) {
            String msg = t.getMessage();
            if (msg != null && msg.contains("invalid_grant")) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }


    /* ============================= */
    /* ===== INTERNAL CONTEXT ====== */
    /* ============================= */

    private static class HandlerContext {
        Context lambdaContext;
        Docs docsClient;
        TaskRepository taskRepository;
        String documentId;
        String myName;
    }
}
