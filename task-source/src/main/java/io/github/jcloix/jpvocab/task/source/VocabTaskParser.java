package io.github.jcloix.jpvocab.task.source;

import io.github.jcloix.jpvocab.domain.NormalizedWord;
import io.github.jcloix.jpvocab.domain.WordNormalizer;
import io.github.jcloix.jpvocab.domain.VocabTask;

import java.util.ArrayList;
import java.util.List;

public class VocabTaskParser {


    public List<VocabTask> parse(List<List<String>> rows) {
        List<VocabTask> tasks = new ArrayList<>();

        // skip header
        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);

            if (row.size() <= GoogleDocTableSchema.EXAMPLE_COL) {
                continue;
            }

            String word = row.get(GoogleDocTableSchema.WORD_COL);
            if (word == null || word.isBlank()) {
                continue;
            }

            NormalizedWord nw = WordNormalizer.normalize(word);

            tasks.add(new VocabTask(
                    i,
                    word,
                    nw,
                    row.get(GoogleDocTableSchema.ASSIGNED_TO_COL),
                    row.get(GoogleDocTableSchema.EXAMPLE_COL)
            ));
        }
        return tasks;
    }
}
