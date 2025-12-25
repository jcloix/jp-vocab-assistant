package io.github.jcloix.jpvocab.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.api.services.docs.v1.Docs;
import io.github.jcloix.jpvocab.config.GoogleDocsAuth;
import io.github.jcloix.jpvocab.model.VocabTask;
import io.github.jcloix.jpvocab.service.TaskDetectionService;
import io.github.jcloix.jpvocab.service.VocabTaskParser;
import io.github.jcloix.jpvocab.service.source.GoogleDocsTaskSource;
import io.github.jcloix.jpvocab.service.source.TaskSource;

import java.util.List;

public class TaskDetectorHandler implements RequestHandler<Void, String> {

    @Override
    public String handleRequest(Void input, Context context) {

        try {
            String credentialsPath = System.getenv("GOOGLE_CREDENTIALS_PATH");
            String documentId = System.getenv("GOOGLE_DOC_ID");
            String myName = System.getenv("MY_NAME");

            Docs docsClient = GoogleDocsAuth.getDocsService(credentialsPath);

            // NEW: explicit wiring
            VocabTaskParser parser = new VocabTaskParser();
            TaskSource source = new GoogleDocsTaskSource(docsClient, documentId, parser);

            List<VocabTask> allTasks = source.fetchTasks();

            TaskDetectionService detector = new TaskDetectionService(myName);
            List<VocabTask> pending = detector.detectPendingTasks(allTasks);

            pending.forEach(task ->
                    context.getLogger().log("Pending task: " + task)
            );

            return "Detected " + pending.size() + " pending tasks";

        } catch (Exception e) {
            context.getLogger().log("ERROR: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
