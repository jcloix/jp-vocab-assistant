package com.julien.jpvocab.service;

import com.julien.jpvocab.model.VocabTask;

import java.util.List;
import java.util.stream.Collectors;

public class TaskDetectionService {

    private final String myName;

    public TaskDetectionService(String myName) {
        this.myName = myName;
    }

    public List<VocabTask> detectPendingTasks(List<VocabTask> allTasks) {
        return allTasks.stream()
                .filter(task -> myName.equals(task.getAssignedTo()))
                .filter(VocabTask::isSentenceMissing)
                .collect(Collectors.toList());
    }
}
