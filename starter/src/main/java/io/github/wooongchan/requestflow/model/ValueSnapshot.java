package io.github.wooongchan.requestflow.model;

import com.fasterxml.jackson.databind.JsonNode;

public final class ValueSnapshot {

    private final String type;
    private final JsonNode value;
    private final boolean truncated;

    public ValueSnapshot(String type, JsonNode value, boolean truncated) {
        this.type = type;
        this.value = value;
        this.truncated = truncated;
    }

    public String getType() {
        return type;
    }

    public JsonNode getValue() {
        return value;
    }

    public boolean isTruncated() {
        return truncated;
    }
}
