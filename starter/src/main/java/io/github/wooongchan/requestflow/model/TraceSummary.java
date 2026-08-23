package io.github.wooongchan.requestflow.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * 목록 화면(GET /trace-viewer/api/traces)에서 사용하는 요약 뷰.
 */
public record TraceSummary(
        String traceId,
        @JsonProperty("method") String httpMethod,
        String path,
        Instant startedAt,
        long durationMs,
        int status) {

    public static TraceSummary from(TraceRecord record) {
        return new TraceSummary(
                record.getTraceId(),
                record.getHttpMethod(),
                record.getPath(),
                record.getStartedAt(),
                record.getDurationMs(),
                record.getStatus());
    }
}
