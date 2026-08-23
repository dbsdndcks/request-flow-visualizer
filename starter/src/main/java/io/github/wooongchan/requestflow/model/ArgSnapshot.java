package io.github.wooongchan.requestflow.model;

import com.fasterxml.jackson.databind.JsonNode;

public final class ArgSnapshot {

    private final String name;
    private final String type;
    private final JsonNode value;
    private final boolean truncated;

    public ArgSnapshot(String name, String type, JsonNode value, boolean truncated) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.truncated = truncated;
    }

    public String getName() {
        return name;
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
