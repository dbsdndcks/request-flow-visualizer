package io.github.wooongchan.requestflow.web;

import io.github.wooongchan.requestflow.aop.TraceContext;
import io.github.wooongchan.requestflow.model.TraceRecord;
import io.github.wooongchan.requestflow.store.TraceRingBuffer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

/**
 * 요청 하나의 생명주기 동안 TraceContext를 열고 닫으며, 종료 시 TraceRingBuffer에 결과를 저장한다.
 * 뷰어 자신의 경로(viewerPath)는 추적 대상에서 제외한다.
 *
 * <p>Spring Boot 2.6(javax.servlet, Spring Framework 5.3) 전용 어댑터 — starter-boot3의 동명 클래스와
 * 로직은 동일하고 서블릿 네임스페이스와 {@code ResponseStatusException} API(5.3은 {@code getStatus()},
 * 6.x는 {@code getStatusCode()})만 다르다.
 */
public class RequestFlowTraceFilter extends OncePerRequestFilter {

    private final TraceRingBuffer store;
    private final String viewerPath;

    public RequestFlowTraceFilter(TraceRingBuffer store, String viewerPath) {
        this.store = store;
        this.viewerPath = viewerPath;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getRequestURI().startsWith(viewerPath)) {
            chain.doFilter(request, response);
            return;
        }

        String traceId = UUID.randomUUID().toString().substring(0, 8);
        TraceRecord record = new TraceRecord(traceId, request.getMethod(), request.getRequestURI(), Instant.now());
        response.setHeader("X-Trace-Id", traceId);

        long startNanos = System.nanoTime();
        TraceContext.startRequest(record);
        try {
            chain.doFilter(request, response);
            record.setStatus(response.getStatus());
        } catch (Exception exception) {
            // 컨트롤러에서 던진 예외는 Boot의 ErrorPageFilter가 이 필터 바깥에서 /error로 포워딩하며 상태코드를
            // 확정하므로, 이 시점의 response.getStatus()는 아직 갱신 전(기본 200)일 수 있다. 최대한 추정한다.
            record.setStatus(resolveStatusOnException(exception, response));
            throw exception;
        } finally {
            record.setDurationMs((System.nanoTime() - startNanos) / 1_000_000);
            TraceContext.endRequest();
            store.add(record);
        }
    }

    private int resolveStatusOnException(Throwable throwable, HttpServletResponse response) {
        if (response.getStatus() != HttpServletResponse.SC_OK) {
            return response.getStatus();
        }
        ResponseStatus responseStatus = AnnotatedElementUtils.findMergedAnnotation(throwable.getClass(), ResponseStatus.class);
        if (responseStatus != null) {
            return responseStatus.value().value();
        }
        if (throwable instanceof ResponseStatusException) {
            return ((ResponseStatusException) throwable).getStatus().value();
        }
        return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }
}
