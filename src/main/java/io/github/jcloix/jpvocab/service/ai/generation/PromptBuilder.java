package io.github.jcloix.jpvocab.service.ai.generation;

import io.github.jcloix.jpvocab.domain.normalization.NormalizedWord;

import java.util.List;

public class PromptBuilder {

    public String buildBatchPrompt(List<NormalizedWord> words) {
        StringBuilder sb = new StringBuilder("""
                あなたは日本語の先生です。
                各単語について、JLPT N3レベルの自然な日本語の例文を3つ作成してください。

                【重要】
                - 単語ごとに必ず区切ってください
                - 出力形式は厳密に守ってください
                - 説明文は禁止

                出力形式（厳守）：
                ### WORD: 単語
                1. 例文
                2. 例文
                3. 例文
                """);

        for (NormalizedWord w : words) {
            sb.append("\n### INPUT\n");
            if (w.fixedPhrase() != null) sb.append("語句: ").append(w.fixedPhrase()).append("\n");
            else sb.append("単語: ").append(w.baseWord()).append("\n");

            if (w.reading() != null) sb.append("読み方: ").append(w.reading()).append("\n");
            if (w.context() != null) sb.append("文脈ヒント: ").append(w.context()).append("\n");
        }

        return sb.toString();
    }
}
