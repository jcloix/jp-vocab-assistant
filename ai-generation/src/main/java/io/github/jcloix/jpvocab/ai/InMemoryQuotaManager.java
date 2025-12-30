package io.github.jcloix.jpvocab.ai;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class InMemoryQuotaManager extends QuotaManager {

    private final Map<String, Integer> counts = new ConcurrentHashMap<>();

    public InMemoryQuotaManager() {
        super(null, null); // DynamoDB not needed
    }

    @Override
    public int getModelCountToday(String model) {
        return counts.getOrDefault(model, 0);
    }

    @Override
    public void incrementModelCounter(String model) {
        counts.merge(model, 1, Integer::sum);
    }
}
