package io.github.jcloix.jpvocab.handler;

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

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        LambdaLogger.init(context);
        LambdaLogger.log("==== DISCORD INTERACTION RECEIVED ====");

        try {
            Map<String, String> headers = (Map<String, String>) input.get("headers");
            String rawBody = (String) input.get("body");

            if (!verifySignature(headers, rawBody)) {
                return errorResponse("invalid signature");
            }

            Map<String, Object> payload = MAPPER.readValue(rawBody, Map.class);
            Integer type = extractInteger(payload.get("type"));

            // Discord PING
            if (type != null && type == 1) {
                return pongResponse();
            }

            return enqueueInteraction(payload);

        } catch (Exception e) {
            LambdaLogger.log("ERROR: " + e.getMessage());
            return errorResponse(e.getMessage());
        }
    }

    private Map<String, Object> enqueueInteraction(Map<String, Object> payload) throws Exception {

        Map<String, Object> data = (Map<String, Object>) payload.get("data");
        if (data == null || data.get("custom_id") == null) {
            return errorResponse("Missing custom_id");
        }

        String customId = (String) data.get("custom_id");
        LambdaLogger.log("custom_id = " + customId);

        Pattern p = Pattern.compile("word_(.+)_row_(\\d+)_choice_(\\d+)");
        Matcher m = p.matcher(customId);
        if (!m.matches()) {
            return errorResponse("Invalid custom_id format");
        }

        Map<String, Object> sqsPayload = new HashMap<>();
        sqsPayload.put("word", m.group(1));
        sqsPayload.put("rowId", Integer.parseInt(m.group(2)));
        sqsPayload.put("choice", Integer.parseInt(m.group(3)));
        sqsPayload.put("token", payload.get("token"));
        sqsPayload.put("applicationId", payload.get("application_id"));

        String body = MAPPER.writeValueAsString(sqsPayload);

        SQS.sendMessage(b -> b
                .queueUrl(QUEUE_URL)
                .messageBody(body)
        );

        // Immediate ACK to Discord
        Map<String, Object> response = new HashMap<>();
        response.put("type", 6); // DEFERRED_UPDATE_MESSAGE
        return response;
    }

    private boolean verifySignature(Map<String, String> headers, String body) {
        try {
            String signatureHex = headers.get("x-signature-ed25519");
            String timestamp = headers.get("x-signature-timestamp");

            if (signatureHex == null || timestamp == null) return false;

            byte[] publicKeyBytes = hexStringToByteArray(DISCORD_PUBLIC_KEY);
            byte[] signatureBytes = hexStringToByteArray(signatureHex);
            byte[] message = (timestamp + body).getBytes(StandardCharsets.UTF_8);

            Ed25519Signer signer = new Ed25519Signer();
            signer.init(false, new Ed25519PublicKeyParameters(publicKeyBytes, 0));
            signer.update(message, 0, message.length);

            return signer.verifySignature(signatureBytes);

        } catch (Exception e) {
            LambdaLogger.log("Signature verification failed: " + e.getMessage());
            return false;
        }
    }

    private Map<String, Object> pongResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("type", 1);
        return response;
    }

    private Map<String, Object> errorResponse(String msg) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("error", msg);
        return response;
    }

    private Integer extractInteger(Object obj) {
        return obj instanceof Number ? ((Number) obj).intValue() : null;
    }

    private byte[] hexStringToByteArray(String s) {
        byte[] data = new byte[s.length() / 2];
        for (int i = 0; i < s.length(); i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
