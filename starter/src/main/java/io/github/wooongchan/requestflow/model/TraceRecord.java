package io.github.wooongchan.requestflow.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * HTTP 요청 1건에 대한 전체 트레이스. roots 아래로 사용자 빈 호출 트리(들)가 매달린다.
 *
 * <p>보통은 root가 하나(진입 컨트롤러)뿐이지만, {@code @RestControllerAdvice} 예외 핸들러처럼
 * 원래 호출 트리가 끝난 뒤 프레임워크가 별도로 호출하는 계측 대상 빈이 있으면 두 번째 root로 추가된다.
 */
public final class TraceRecord {

    private final String traceId;

    @JsonProperty("method")
    private final String httpMethod;

    private final String path;
    private final Instant startedAt;

    private long durationMs;
    private int status;
    private final List<TraceNode> roots = new ArrayList<>();

    public TraceRecord(String traceId, String httpMethod, String path, Instant startedAt) {
        this.traceId = traceId;
        this.httpMethod = httpMethod;
        this.path = path;
        this.startedAt = startedAt;
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

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public List<TraceNode> getRoots() {
        return roots;
    }

    public void addRoot(TraceNode root) {
        roots.add(root);
    }
}
