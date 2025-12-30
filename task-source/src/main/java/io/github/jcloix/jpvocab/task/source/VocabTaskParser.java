package io.github.jcloix.jpvocab.task.source;

import io.github.jcloix.jpvocab.domain.NormalizedWord;
import io.github.jcloix.jpvocab.domain.WordNormalizer;
import io.github.jcloix.jpvocab.domain.VocabTask;

import java.util.ArrayList;
import java.util.List;

public class VocabTaskParser {

    private static final int WORD_COL = 3;
    private static final int ASSIGNED_TO_COL = 4;
    private static final int EXAMPLE_COL = 5;

    public List<VocabTask> parse(List<List<String>> rows) {
        List<VocabTask> tasks = new ArrayList<>();

        // skip header
        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);

            if (row.size() <= EXAMPLE_COL) {
                continue;
            }

            String word = row.get(WORD_COL);
            if (word == null || word.isBlank()) {
                continue;
            }

            NormalizedWord nw = WordNormalizer.normalize(word);

            tasks.add(new VocabTask(
                    i,
                    word,
                    nw,
                    row.get(ASSIGNED_TO_COL),
                    row.get(EXAMPLE_COL)
            ));
        }
        return tasks;
    }
}
