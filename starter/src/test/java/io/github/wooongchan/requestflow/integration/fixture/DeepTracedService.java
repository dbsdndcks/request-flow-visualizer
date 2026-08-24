package io.github.wooongchan.requestflow.integration.fixture;

import io.github.wooongchan.requestflow.annotation.DeepTrace;
import org.springframework.stereotype.Service;

/**
 * public 메서드가 private 메서드를 내부 호출(self-invocation)하는 구조.
 * {@code @DeepTrace}가 없었다면 validate/transform 호출은 트레이스에 잡히지 않는다.
 */
@DeepTrace
@Service
public class DeepTracedService {

    public String process(String input) {
        validate(input);
        return transform(input);
    }

    private void validate(String input) {
        if ("boom".equals(input)) {
            throw new IllegalStateException("boom");
        }
    }

    private String transform(String input) {
        return "processed:" + input;
    }
}
