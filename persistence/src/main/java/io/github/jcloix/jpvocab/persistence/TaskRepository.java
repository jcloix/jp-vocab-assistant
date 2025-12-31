package io.github.jcloix.jpvocab.persistence;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class TaskRepository {

    private final DynamoDbClient dynamoDb;
    private final String tableName;

    public TaskRepository() {
        this.dynamoDb = DynamoDbClient.create();
        this.tableName = System.getenv("TASKS_TABLE_NAME");
    }

    private String pk(String docId) {
        return "DOC#" + docId;
    }

    private String sk(String word, int rowId) {
        return "WORD#" + word + "#ROW#" + rowId;
    }

    /**
     * Find a task by word, picking the closest rowId if multiple exist.
     */
    public Optional<PersistedTask> findByWord(String docId, String word, int approxRowId) {
        QueryResponse query = dynamoDb.query(q -> q
                .tableName(tableName)
                .keyConditionExpression("PK = :pk and begins_with(SK, :skPrefix)")
                .expressionAttributeValues(Map.of(
                        ":pk", AttributeValue.fromS(pk(docId)),
                        ":skPrefix", AttributeValue.fromS("WORD#" + word)
                ))
        );

        List<PersistedTask> tasks = query.items().stream()
                .map(this::mapToTask)
                .collect(Collectors.toList());
        if (tasks.isEmpty()) return Optional.empty();

        // Pick the one with rowId closest to approxRowId
        tasks.sort(Comparator.comparingInt(t -> Math.abs(t.getRowId() - approxRowId)));
        return Optional.of(tasks.get(0));
    }

    /**
     * Save a new task with ordered choices.
     */
    public void saveNewTask(String docId, int rowId, String word, List<String> choices) {
        Instant now = Instant.now();

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("PK", AttributeValue.fromS(pk(docId)));
        item.put("SK", AttributeValue.fromS(sk(word, rowId)));
        item.put("word", AttributeValue.fromS(word));
        item.put("rowId", AttributeValue.fromN(String.valueOf(rowId)));
        item.put("status", AttributeValue.fromS("PENDING"));

        // Preserve order using a List attribute instead of a Set
        item.put("choices", AttributeValue.fromL(
                choices.stream()
                        .map(AttributeValue::fromS)
                        .toList()
        ));

        item.put("createdAt", AttributeValue.fromS(now.toString()));
        item.put("updatedAt", AttributeValue.fromS(now.toString()));

        dynamoDb.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .conditionExpression("attribute_not_exists(PK)")
                .build());
    }

    public void markNotified(String docId, int rowId, String word) {
        updateStatus(docId, word, rowId, "NOTIFIED", null);
    }

    public void markDone(String docId, int rowId, String word, int selectedChoice) {
        updateStatus(docId, word, rowId, "DONE", selectedChoice);
    }

    private void updateStatus(String docId, String word, int rowId, String status, Integer selectedChoice) {
        Map<String, String> attrNames = new HashMap<>();
        attrNames.put("#S", "status");

        Map<String, AttributeValue> attrValues = new HashMap<>();
        attrValues.put(":status", AttributeValue.fromS(status));
        attrValues.put(":updatedAt", AttributeValue.fromS(Instant.now().toString()));

        String update = "SET #S = :status, updatedAt = :updatedAt";

        if (selectedChoice != null) {
            attrValues.put(":choice", AttributeValue.fromN(selectedChoice.toString()));
            update += ", selectedChoice = :choice";
        }

        dynamoDb.updateItem(UpdateItemRequest.builder()
                .tableName(tableName)
                .key(Map.of(
                        "PK", AttributeValue.fromS(pk(docId)),
                        "SK", AttributeValue.fromS(sk(word, rowId))
                ))
                .updateExpression(update)
                .expressionAttributeNames(attrNames)
                .expressionAttributeValues(attrValues)
                .build());
    }

    private PersistedTask mapToTask(Map<String, AttributeValue> item) {
        // Read choices as a list to preserve order
        List<String> choices = item.get("choices").l().stream()
                .map(AttributeValue::s)
                .toList();

        return new PersistedTask(
                item.get("PK").s().substring(4), // remove "DOC#"
                Integer.parseInt(item.get("rowId").n()),
                item.get("word").s(),
                choices,
                item.get("status").s(),
                Instant.parse(item.get("createdAt").s()),
                Instant.parse(item.get("updatedAt").s())
        );
    }
}
