package com.todo.eod.app;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryFeatureFlagProvider implements FeatureFlagProvider {
    private final ConcurrentHashMap<String, Integer> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Integer> getPercentage(String flagKey) {
        if (flagKey == null) return Optional.empty();
        return Optional.ofNullable(store.get(flagKey));
    }

    @Override
    public void setPercentage(String flagKey, int percentage) {
        if (flagKey == null) return;
        if (percentage < 0) percentage = 0;
        if (percentage > 100) percentage = 100;
        store.put(flagKey, percentage);
    }
}

