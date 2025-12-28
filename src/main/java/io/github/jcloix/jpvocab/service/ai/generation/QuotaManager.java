package io.github.jcloix.jpvocab.service.ai.generation;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeAction;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.time.LocalDate;
import java.util.Map;

public class QuotaManager {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public QuotaManager(DynamoDbClient dynamoDbClient, String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    public int getModelCountToday(String model) {
        String today = LocalDate.now().toString();

        Map<String, AttributeValue> key = Map.of(
                "modelName", AttributeValue.builder().s(model).build(),
                "date", AttributeValue.builder().s(today).build()
        );

        GetItemResponse response = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .build());

        if (response.hasItem() && response.item().containsKey("count")) {
            return Integer.parseInt(response.item().get("count").n());
        }
        return 0;
    }

    public void incrementModelCounter(String model) {
        String today = LocalDate.now().toString();

        Map<String, AttributeValue> key = Map.of(
                "modelName", AttributeValue.builder().s(model).build(),
                "date", AttributeValue.builder().s(today).build()
        );

        Map<String, AttributeValueUpdate> updates = Map.of(
                "count", AttributeValueUpdate.builder()
                        .action(AttributeAction.ADD)
                        .value(AttributeValue.builder().n("1").build())
                        .build()
        );

        dynamoDbClient.updateItem(UpdateItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .attributeUpdates(updates)
                .build());
    }
}
