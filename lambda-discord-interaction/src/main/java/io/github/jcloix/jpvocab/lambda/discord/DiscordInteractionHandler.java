package io.github.jcloix.jpvocab.lambda.discord;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jcloix.jpvocab.util.LambdaLogger;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscordInteractionHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DISCORD_PUBLIC_KEY = System.getenv("DISCORD_PUBLIC_KEY");
    private static final String QUEUE_URL = System.getenv("DISCORD_TASK_QUEUE_URL");
    private static final SqsClient SQS = SqsClient.create();
    private static final byte[] DISCORD_PUBLIC_KEY_BYTES = hexStringToByteArrayStatic(DISCORD_PUBLIC_KEY);
    private static final Pattern CUSTOM_ID_PATTERN = Pattern.compile("word_(.+)_row_(\\d+)_choice_(\\d+)");

    // Static warm-up block
    static {
        long t0 = System.nanoTime();

        // Jackson warm-up
        try {
            MAPPER.writeValueAsString(Map.of("x", 1));
            MAPPER.readValue("{\"x\":1}", Map.class);
        } catch (Exception ignored) {}
        long t1 = System.nanoTime();
        System.out.println("Jackson static warm-up took " + (t1 - t0) / 1_000_000 + " ms");

        long t2 = System.nanoTime();

        // SQS client warm-up: send dummy message to pre-initialize HTTP/TLS
        try {
            SQS.sendMessage(b -> b
                    .queueUrl(QUEUE_URL)
                    .messageBody("{\"warmup\":true}")
                    .delaySeconds(0)
            );
        } catch (Exception ignored) {}
        long t3 = System.nanoTime();
        System.out.println("SQS static init / warm-up took " + (t3 - t2) / 1_000_000 + " ms");

        long t4 = System.nanoTime();

        // BouncyCastle warm-up
        Ed25519Signer s = new Ed25519Signer();
        long t5 = System.nanoTime();
        System.out.println("BouncyCastle static init took " + (t5 - t4) / 1_000_000 + " ms");
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        LambdaLogger.init(context);
        long t0 = System.nanoTime();
        LambdaLogger.log("==== DISCORD INTERACTION RECEIVED ====");

        try {
            Map<String, String> headers = (Map<String, String>) input.get("headers");
            String rawBody = (String) input.get("body");

            long tSigStart = System.nanoTime();
            boolean valid = verifySignature(headers, rawBody);
            long tSigEnd = System.nanoTime();
            LambdaLogger.log("verifySignature took " + ms(tSigEnd - tSigStart) + " ms");

            if (!valid) {
                return errorResponse("invalid signature");
            }

            long tParseStart = System.nanoTime();
            Map<String, Object> payload = MAPPER.readValue(rawBody, Map.class);
            long tParseEnd = System.nanoTime();
            LambdaLogger.log("Jackson parse took " + ms(tParseEnd - tParseStart) + " ms");

            Integer type = extractInteger(payload.get("type"));
            if (type != null && type == 1) {
                LambdaLogger.log("PING detected");
                return pongResponse(rawBody);
            }

            Map<String, Object> response = enqueueInteraction(payload);

            long tEnd = System.nanoTime();
            LambdaLogger.log("TOTAL handleRequest took " + ms(tEnd - t0) + " ms");
            return response;

        } catch (Exception e) {
            LambdaLogger.log("ERROR: " ,e);
            return errorResponse(e.getMessage());
        }
    }

    private Map<String, Object> enqueueInteraction(Map<String, Object> payload) throws Exception {
        long t0 = System.nanoTime();

        Map<String, Object> data = (Map<String, Object>) payload.get("data");
        if (data == null || data.get("custom_id") == null) {
            return errorResponse("Missing custom_id");
        }

        String customId = (String) data.get("custom_id");
        LambdaLogger.log("custom_id = " + customId);

        long tRegexStart = System.nanoTime();
        Matcher m = CUSTOM_ID_PATTERN.matcher(customId);
        if (!m.matches()) {
            return errorResponse("Invalid custom_id format");
        }
        long tRegexEnd = System.nanoTime();
        LambdaLogger.log("Regex parse took " + ms(tRegexEnd - tRegexStart) + " ms");

        Map<String, Object> sqsPayload = new HashMap<>();
        sqsPayload.put("word", m.group(1));
        sqsPayload.put("rowId", Integer.parseInt(m.group(2)));
        sqsPayload.put("choice", Integer.parseInt(m.group(3)));
        sqsPayload.put("token", payload.get("token"));
        sqsPayload.put("applicationId", payload.get("application_id"));

        long tSerStart = System.nanoTime();
        String body = MAPPER.writeValueAsString(sqsPayload);
        long tSerEnd = System.nanoTime();
        LambdaLogger.log("Jackson serialize took " + ms(tSerEnd - tSerStart) + " ms");

        long tSqsStart = System.nanoTime();
        SQS.sendMessage(b -> b
                .queueUrl(QUEUE_URL)
                .messageBody(body)
        );
        long tSqsEnd = System.nanoTime();
        LambdaLogger.log("SQS sendMessage took " + ms(tSqsEnd - tSqsStart) + " ms");

        long tEnd = System.nanoTime();
        LambdaLogger.log("enqueueInteraction total took " + ms(tEnd - t0) + " ms");

        return httpJsonResponse(200, Map.of("type", 6));
    }

    private boolean verifySignature(Map<String, String> headers, String body) {
        try {
            String signatureHex = headers.get("x-signature-ed25519");
            String timestamp = headers.get("x-signature-timestamp");

            if (signatureHex == null || timestamp == null) return false;

            byte[] signatureBytes = hexStringToByteArray(signatureHex);
            byte[] message = (timestamp + body).getBytes(StandardCharsets.UTF_8);

            Ed25519Signer signer = new Ed25519Signer();
            signer.init(false, new Ed25519PublicKeyParameters(DISCORD_PUBLIC_KEY_BYTES, 0));
            signer.update(message, 0, message.length);

            return signer.verifySignature(signatureBytes);

        } catch (Exception e) {
            LambdaLogger.log("Signature verification failed: " + e.getMessage());
            return false;
        }
    }

    private Map<String, Object> httpJsonResponse(int statusCode, Object body) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("statusCode", statusCode);
            response.put("headers", Map.of(
                    "Content-Type", "application/json"
            ));
            response.put("body", MAPPER.writeValueAsString(body));
            return response;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Object> pongResponse(String body) {
        return httpJsonResponse(200, Map.of("type", 1));
    }

    private Map<String, Object> errorResponse(String msg) {
        return httpJsonResponse(401, Map.of(
                "error", msg
        ));
    }

    private Integer extractInteger(Object obj) {
        return obj instanceof Number ? ((Number) obj).intValue() : null;
    }

    private static byte[] hexStringToByteArrayStatic(String s) {
        byte[] data = new byte[s.length() / 2];
        for (int i = 0; i < s.length(); i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    private byte[] hexStringToByteArray(String s) {
        return hexStringToByteArrayStatic(s);
    }

    private static long ms(long nanos) {
        return nanos / 1_000_000;
    }
}
