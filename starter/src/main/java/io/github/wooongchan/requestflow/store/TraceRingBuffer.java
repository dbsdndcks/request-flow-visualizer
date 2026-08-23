package io.github.wooongchan.requestflow.store;

import io.github.wooongchan.requestflow.model.TraceRecord;
import io.github.wooongchan.requestflow.model.TraceSummary;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 최근 N개의 요청 트레이스만 보관하는 인메모리 링버퍼.
 * 로컬 개발 중 요청 1건을 확인하는 용도이므로 영구 저장소는 사용하지 않는다.
 */
public class TraceRingBuffer {

    private final int capacity;
    private final LinkedList<TraceRecord> records = new LinkedList<>();

    public TraceRingBuffer(int capacity) {
        this.capacity = capacity;
    }

    public synchronized void add(TraceRecord record) {
        records.addFirst(record);
        while (records.size() > capacity) {
            records.removeLast();
        }
    }

    public synchronized List<TraceSummary> list(int limit) {
        return records.stream()
                .limit(Math.max(limit, 0))
                .map(TraceSummary::from)
                .collect(Collectors.toList());
    }

    public synchronized Optional<TraceRecord> findById(String traceId) {
        return records.stream()
                .filter(record -> record.getTraceId().equals(traceId))
                .findFirst();
    }

    public synchronized void clear() {
        records.clear();
    }
}
