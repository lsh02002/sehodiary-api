package com.shop.sehodiary_api.repository.activity.logger;

// Loggable.java
public interface Loggable {
    default String logMessage() {
        return null;
    }
}

