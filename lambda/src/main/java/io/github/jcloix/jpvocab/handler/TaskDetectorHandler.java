package com.julien.jpvocab.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.julien.jpvocab.model.VocabTask;
import com.julien.jpvocab.service.TaskDetectionService;

import java.util.List;

public class TaskDetectorHandler implements RequestHandler<Void, String> {

    @Override
    public String handleRequest(Void input, Context context) {

        // Temporary mock data (Phase 1)
        List<VocabTask> mockTasks = List.of(
                new VocabTask("勉強", "Julien", true),
                new VocabTask("旅行", "SomeoneElse", true),
                new VocabTask("日本語", "Julien", false)
        );

        TaskDetectionService detector = new TaskDetectionService("Julien");
        List<VocabTask> pending = detector.detectPendingTasks(mockTasks);

        pending.forEach(task ->
                context.getLogger().log("Pending task: " + task)
        );

        return "Detected " + pending.size() + " pending tasks";
    }
}
