package io.github.jcloix.jpvocab.lambda.completion;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jcloix.jpvocab.notification.DiscordNotifier;
import io.github.jcloix.jpvocab.persistence.PersistedTask;
import io.github.jcloix.jpvocab.persistence.TaskRepository;
import io.github.jcloix.jpvocab.util.LambdaLogger;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;

import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscordResponseHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DISCORD_PUBLIC_KEY = System.getenv("DISCORD_PUBLIC_KEY"); // raw hex key

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        LambdaLogger.init(context);
        LambdaLogger.log("==== TOP OF HANDLE REQUEST ====");
        LambdaLogger.log("RAW INPUT: " + input);

        try {
            Map<String, String> headers = ((Map<String, String>) input.get("headers"));
            String rawBody = (String) input.get("body");

            if (!verifySignature(headers, rawBody, context)) {
                return errorResponse("invalid signature");
            }

            Map<String, Object> payload = MAPPER.readValue(rawBody, Map.class);
            Integer type = extractInteger(payload.get("type"));

            if (type != null && type == 1) {
                return pongResponse(context);
            }

            return handleDiscordInteraction(payload, context);

        } catch (Exception e) {
            LambdaLogger.log("ERROR: " + e.getMessage());
            return errorResponse(e.getMessage());
        }
    }

    private boolean verifySignature(Map<String, String> headers, String body, Context context) {
        try {
            String signatureHex = headers.get("x-signature-ed25519");
            String timestamp = headers.get("x-signature-timestamp");

            if (signatureHex == null || timestamp == null) {
                LambdaLogger.log("Missing signature/timestamp");
                return false;
            }

            byte[] publicKeyBytes = hexStringToByteArray(DISCORD_PUBLIC_KEY);
            byte[] signatureBytes = hexStringToByteArray(signatureHex);
            byte[] message = (timestamp + (body != null ? body : "")).getBytes(StandardCharsets.UTF_8);

            Ed25519PublicKeyParameters pubKeyParams = new Ed25519PublicKeyParameters(publicKeyBytes, 0);
            Ed25519Signer signer = new Ed25519Signer();
            signer.init(false, pubKeyParams);
            signer.update(message, 0, message.length);

            boolean valid = signer.verifySignature(signatureBytes);
            if (!valid) LambdaLogger.log("Invalid Discord signature");
            return valid;

        } catch (Exception e) {
            LambdaLogger.log("Signature verification error: " + e.getMessage());
            return false;
        }
    }

    private Map<String, Object> handleDiscordInteraction(Map<String, Object> payload, Context context) {
        try {
            // --- 1. Extract custom_id and parse it ---
            Map<String, Object> data = (Map<String, Object>) payload.get("data");
            if (data == null || data.get("custom_id") == null) {
                LambdaLogger.log("Data field or custom_id is null!");
                return errorResponse("Invalid interaction data");
            }

            String customId = (String) data.get("custom_id");
            LambdaLogger.log("Data-level custom_id: " + customId);

            Pattern p = Pattern.compile("word_(.+)_row_(\\d+)_choice_(\\d+)");
            Matcher m = p.matcher(customId);
            if (!m.matches()) {
                LambdaLogger.log("custom_id does not match expected pattern: " + customId);
                return errorResponse("Invalid custom_id format");
            }
            String word = m.group(1);
            int rowId = Integer.parseInt(m.group(2));
            int selectedChoice = Integer.parseInt(m.group(3));

            // --- 2. DEFER response to Discord immediately ---
            Map<String, Object> deferredResponse = new HashMap<>();
            deferredResponse.put("type", 6); // DEFERRED_UPDATE_MESSAGE
            LambdaLogger.log("Sending deferred response to Discord for word: " + word);

            // --- 3. Do the processing asynchronously ---
            Runnable task = () -> processDiscordChoice(word, rowId, selectedChoice);
            new Thread(task).start(); // simple async in Lambda; you could use ExecutorService

            return deferredResponse;

        } catch (Exception e) {
            return errorResponse(e.getMessage());
        }
    }

    private void processDiscordChoice(String word, int rowId, int selectedChoice) {
        LambdaLogger.log("Lambda Deferred thread started");
        try {
            String documentId = System.getenv("GOOGLE_DOC_ID");

            TaskRepository taskRepository = new TaskRepository();
            Optional<PersistedTask> taskOpt = taskRepository.findByWord(documentId, word, rowId);
            if (taskOpt.isEmpty()) {
                LambdaLogger.log("Task not found for word: " + word);
                safeSendDiscordMessage("⚠ Task not found for word: " + word);
                return;
            }


            PersistedTask task = taskOpt.get();
            taskRepository.markDone(documentId, task.getRowId(), task.getWord(), selectedChoice);


            String ackMessage = String.format("✅ You selected choice %d for word: %s", selectedChoice + 1, word);
            safeSendDiscordMessage(ackMessage);

        } catch (Exception e) {
            LambdaLogger.log("Error processing Discord choice: " + e.getMessage());
            safeSendDiscordMessage("⚠ Error processing your choice for word: " + word);
        }
    }

    /** Wrapper to safely send a Discord message without throwing */
    private void safeSendDiscordMessage(String message) {
        try {

            LambdaLogger.log("Send to discord:"+message);
            DiscordNotifier.sendMessage(message);
        } catch (Exception e) {
            LambdaLogger.log("Failed to send Discord message: " + e.getMessage());
        }
    }



    private Map<String, Object> pongResponse(Context context) {
        LambdaLogger.log("PING received → returning PONG");
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
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }


}
