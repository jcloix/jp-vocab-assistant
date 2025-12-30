package io.github.jcloix.jpvocab.notification;

import io.github.jcloix.jpvocab.util.LambdaLogger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;

@Tag("integration")
@EnabledIfEnvironmentVariable(named = "DISCORD_WEBHOOK_URL", matches = ".+")
class DiscordNotifierIT {

    // -----------------------------
    // All tests converted
    // -----------------------------

    @Test
    void testMultiLineWord() throws Exception {
        runFailingTest(
                "MULTILINE WORD",
                "放す\nはな",
                List.of(
                        "先生が「始めて」と言うまで、ペンを放さないでください。",
                        "捕まえた魚を、すぐに川に放してあげました。",
                        "飼い主が犬のリードをうっかり放してしまい、犬は走り去ってしまった。"
                )
        );
    }

    @Test
    @Disabled
    void testParenthesesInWord() throws Exception {
        runTest(
                "PARENTHESES IN WORD",
                "厳しい（ものがある）",
                List.of(
                        "この仕事には厳しい面がある。",
                        "彼の指導は厳しいが、とても勉強になる。",
                        "自然のルールは時に厳しい。"
                )
        );
    }

    @Test
    @Disabled
    void testQuotesAndEmoji() throws Exception {
        runTest(
                "QUOTES + EMOJI",
                "価値",
                List.of(
                        "「努力」には大きな価値があると思います。",
                        "この経験は将来にとって貴重な価値になる。",
                        "時間の価値を大切にしましょう⏰"
                )
        );
    }

    @Test
    @Disabled
    void testNearLimitSentence() throws Exception {
        runTest(
                "NEAR LIMIT SENTENCE",
                "環境",
                List.of(
                        "環境を守るための活動は、すぐに結果が出なくても、将来のために大きな価値があると信じられています。",
                        "環境問題について真剣に考える必要があります。",
                        "私たちは環境に優しい生活を心がけるべきです。"
                )
        );
    }

    @Test
    @Disabled
    void testVeryLongButton() throws Exception {
        runTest(
                "VERY LONG BUTTON",
                "長い文",
                List.of(
                        "これはDiscordのボタンラベルが八十文字を大きく超えた場合にJSONは正しくてもAPI側で拒否されるかどうかを確実に検証するために作られた非常に非常に長いテスト文です。",
                        "短い文です。",
                        "これも問題ないはずです。"
                )
        );
    }

    @Test
    @Disabled
    void testQuotesSingleAndDouble() throws Exception {
        runTest(
                "SINGLE + DOUBLE QUOTES",
                "引用",
                List.of(
                        "彼は「\"成功\"とは努力の積み重ねだ」と言った。",
                        "先生は'今すぐ始めなさい'と強く言いました。",
                        "彼女は\"できると信じて\"前に進んだ。"
                )
        );
    }

    // === Choice count edge cases ===

    @Test
    @Disabled
    void testNoChoice() {
        runFailingTest(
                "NO CHOICE (EXPECTED FAILURE)",
                "選択なし",
                List.of()
        );
    }

    @Test
    @Disabled
    void testOneChoice() throws Exception {
        runTest(
                "ONE CHOICE",
                "唯一",
                List.of("これは唯一の選択肢です。")
        );
    }

    @Test
    @Disabled
    void testFourChoices() throws Exception {
        runTest(
                "FOUR CHOICES",
                "最大",
                List.of(
                        "第一の選択肢です。",
                        "第二の選択肢です。",
                        "第三の選択肢です。",
                        "第四の選択肢です。"
                )
        );
    }

    @Test
    @Disabled
    void testFiveChoices() throws Exception {
        runTest(
                "FIVE CHOICES (TRUNCATION EXPECTED)",
                "超過",
                List.of(
                        "第一の選択肢です。",
                        "第二の選択肢です。",
                        "第三の選択肢です。",
                        "第四の選択肢です。",
                        "第五の選択肢（表示されないはず）"
                )
        );
    }

    // -----------------------------
    // Test helpers
    // -----------------------------

    private void runTest(String label, String word, List<String> choices) throws Exception {
        LambdaLogger.log("\n==============================");
        LambdaLogger.log("▶ TEST: " + label);
        LambdaLogger.log("Word: " + word.replace("\n", "\\n"));
        LambdaLogger.log("Choices: " + choices.size());
        LambdaLogger.log("==============================");

        DiscordNotifier.sendTaskMessage(word, word, 999, choices);
        Thread.sleep(1500);
    }

    private void runFailingTest(String label, String word, List<String> choices) {
        LambdaLogger.log("\n==============================");
        LambdaLogger.log("▶ TEST: " + label);
        LambdaLogger.log("==============================");

        try {
            DiscordNotifier.sendTaskMessage(word, word, 999, choices);
            throw new AssertionError("Expected failure, but message was sent");
        } catch (Exception e) {
            LambdaLogger.log("✅ Expected failure: " + e.getMessage());
        }
    }
}
