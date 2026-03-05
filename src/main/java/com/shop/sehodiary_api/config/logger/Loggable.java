package com.shop.sehodiary_api.config.logger;

// Loggable.java
public interface Loggable {
    default String logMessage() {
        return null;
    }
}

