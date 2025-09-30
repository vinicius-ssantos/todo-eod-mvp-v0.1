package com.todo.eod.app;

import java.util.Optional;

public interface FeatureFlagProvider {
    Optional<Integer> getPercentage(String flagKey);
    void setPercentage(String flagKey, int percentage);
}

