package com.julien.jpvocab.service;

import com.julien.jpvocab.model.VocabTask;
import java.util.List;

public interface GoogleDocsService {

    List<VocabTask> fetchTasks(String documentId);
}
