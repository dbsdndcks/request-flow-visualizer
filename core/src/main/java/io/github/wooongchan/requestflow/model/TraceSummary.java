package io.github.wooongchan.requestflow.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * 목록 화면(GET /trace-viewer/api/traces)에서 사용하는 요약 뷰.
 *
 * <p>Java 8 호환을 위해 record 대신 일반 클래스로 작성한다(core는 Java 8 타깃).
 */
public final class TraceSummary {

    private final String traceId;

    @JsonProperty("method")
    private final String httpMethod;

    private final String path;
    private final Instant startedAt;
    private final long durationMs;
    private final int status;

    public TraceSummary(String traceId, String httpMethod, String path, Instant startedAt,
                         long durationMs, int status) {
        this.traceId = traceId;
        this.httpMethod = httpMethod;
        this.path = path;
        this.startedAt = startedAt;
        this.durationMs = durationMs;
        this.status = status;
    }

    public static TraceSummary from(TraceRecord record) {
        return new TraceSummary(
                record.getTraceId(),
                record.getHttpMethod(),
                record.getPath(),
                record.getStartedAt(),
                record.getDurationMs(),
                record.getStatus());
    }

    public String getTraceId() {
        return traceId;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getPath() {
        return path;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public int getStatus() {
        return status;
    }
}
