package io.github.wooongchan.requestflow.store;

import io.github.wooongchan.requestflow.model.TraceRecord;
import io.github.wooongchan.requestflow.model.TraceSummary;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TraceRingBufferTest {

    @Test
    void evictsOldestWhenCapacityExceeded() {
        TraceRingBuffer buffer = new TraceRingBuffer(2);
        buffer.add(record("t1"));
        buffer.add(record("t2"));
        buffer.add(record("t3"));

        List<TraceSummary> summaries = buffer.list(10);

        assertThat(summaries).hasSize(2);
        assertThat(summaries).extracting("traceId").containsExactly("t3", "t2");
        assertThat(buffer.findById("t1")).isEmpty();
    }

    @Test
    void findsById() {
        TraceRingBuffer buffer = new TraceRingBuffer(5);
        buffer.add(record("abc"));

        assertThat(buffer.findById("abc")).isPresent();
        assertThat(buffer.findById("missing")).isEmpty();
    }

    @Test
    void clearRemovesAllRecords() {
        TraceRingBuffer buffer = new TraceRingBuffer(5);
        buffer.add(record("a"));
        buffer.add(record("b"));

        buffer.clear();

        assertThat(buffer.list(10)).isEmpty();
    }

    private TraceRecord record(String traceId) {
        return new TraceRecord(traceId, "GET", "/api/x", Instant.now());
    }
}
