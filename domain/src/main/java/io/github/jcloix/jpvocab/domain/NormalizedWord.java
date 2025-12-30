package io.github.jcloix.jpvocab.domain;

/**
 * Value object representing a normalized Japanese vocabulary entry.
 *
 * <p>This is a derived representation used for AI prompting and messaging.
 * It MUST NOT be persisted and MUST NOT contain behavior.
 */
public final class NormalizedWord {

    private final String baseWord;     // Kanji or main expression (e.g. 放す, 外す)
    private final String reading;      // Kana reading if provided (e.g. はな)
    private final String context;      // Context before the word (e.g. メンバーから)
    private final String fixedPhrase;  // Required suffix (e.g. ものがある)

    public NormalizedWord(
            String baseWord,
            String reading,
            String context,
            String fixedPhrase
    ) {
        this.baseWord = baseWord;
        this.reading = reading;
        this.context = context;
        this.fixedPhrase = fixedPhrase;
    }

    public String baseWord() {
        return baseWord;
    }

    public String reading() {
        return reading;
    }

    public String context() {
        return context;
    }

    public String fixedPhrase() {
        return fixedPhrase;
    }

    public boolean hasReading() {
        return reading != null && !reading.isBlank();
    }

    public boolean hasContext() {
        return context != null && !context.isBlank();
    }

    public boolean hasFixedPhrase() {
        return fixedPhrase != null && !fixedPhrase.isBlank();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(baseWord != null ? baseWord : "null");

        if (hasReading()) {
            sb.append("#").append(reading);
        }
        if (hasContext()) {
            sb.append("#ctx:").append(context.replace("\n", "\\n"));
        }
        if (hasFixedPhrase()) {
            sb.append("#fix:").append(fixedPhrase.replace("\n", "\\n"));
        }

        return sb.toString();
    }
}
