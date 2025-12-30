package io.github.jcloix.jpvocab.domain;

public final class WordNormalizer {

    public static NormalizedWord normalize(String raw) {
        if (raw == null) return new NormalizedWord("", null, null, null);

        raw = raw.trim();

        // Kanji + reading on separate line
        String[] lines = raw.split("\\R+");
        if (lines.length == 2 && lines[1].matches("^[ぁ-んー]+$")) {
            return new NormalizedWord(lines[0], lines[1], null, null);
        }

        // （context）Word
        if (raw.startsWith("（") && raw.contains("）")) {
            int end = raw.indexOf("）");
            return new NormalizedWord(
                    raw.substring(end + 1),
                    null,
                    raw.substring(1, end),
                    null
            );
        }

        // Word（fixed phrase）
        if (raw.contains("（") && raw.endsWith("）")) {
            int start = raw.indexOf("（");
            return new NormalizedWord(
                    raw.substring(0, start),
                    null,
                    null,
                    raw.substring(start + 1, raw.length() - 1)
            );
        }

        return new NormalizedWord(raw, null, null, null);
    }
}

