package io.github.wooongchan.requestflow.model;

import java.util.List;

public final class ExceptionSnapshot {

    private final String type;
    private final String message;
    private final List<String> stackTraceTop;

    public ExceptionSnapshot(String type, String message, List<String> stackTraceTop) {
        this.type = type;
        this.message = message;
        this.stackTraceTop = stackTraceTop;
    }

    public static ExceptionSnapshot from(Throwable throwable, int maxStackLines) {
        StackTraceElement[] elements = throwable.getStackTrace();
        int limit = Math.min(maxStackLines, elements.length);
        List<String> top = new java.util.ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            top.add(elements[i].toString());
        }
        return new ExceptionSnapshot(throwable.getClass().getName(), throwable.getMessage(), top);
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public List<String> getStackTraceTop() {
        return stackTraceTop;
    }
}
