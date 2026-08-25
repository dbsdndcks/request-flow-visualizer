package io.github.wooongchan.requestflow.web;

import io.github.wooongchan.requestflow.model.TraceRecord;
import io.github.wooongchan.requestflow.model.TraceSummary;
import io.github.wooongchan.requestflow.store.TraceRingBuffer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${request-flow.viewer-path:/trace-viewer}/api/traces")
public class TraceApiController {

    private final TraceRingBuffer store;

    public TraceApiController(TraceRingBuffer store) {
        this.store = store;
    }

    @GetMapping
    public List<TraceSummary> list(@RequestParam(defaultValue = "50") int limit) {
        return store.list(limit);
    }

    @GetMapping("/{traceId}")
    public ResponseEntity<TraceRecord> get(@PathVariable String traceId) {
        return store.findById(traceId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping
    public ResponseEntity<Void> clear() {
        store.clear();
        return ResponseEntity.noContent().build();
    }
}
