package io.github.wooongchan.requestflow.agent;

import io.github.wooongchan.requestflow.aop.TraceContext;
import io.github.wooongchan.requestflow.capture.ArgSnapshotBuilder;
import io.github.wooongchan.requestflow.capture.ValueSerializer;
import io.github.wooongchan.requestflow.model.ExceptionSnapshot;
import io.github.wooongchan.requestflow.model.TraceNode;

import java.lang.reflect.Method;

/**
 * {@link DeepTraceAdvice}는 ByteBuddy가 대상 메서드 본문에 직접 인라인하는 바이트코드라
 * Spring 빈(ValueSerializer)을 주입받을 수 없다. 이 클래스가 정적 브리지 역할을 한다 —
 * RequestFlowAutoConfiguration이 ValueSerializer 빈을 만들 때 {@link #register}로 한 번 꽂아둔다.
 */
public final class DeepTraceRuntime {

    private static final int MAX_STACK_LINES = 5;

    private static volatile ValueSerializer valueSerializer;

    private DeepTraceRuntime() {
    }

    public static void register(ValueSerializer serializer) {
        valueSerializer = serializer;
    }

    /**
     * 계측 대상 메서드 진입 시 호출된다. 진행 중인 요청 트레이스가 없으면(TraceContext 비활성)
     * 아무 것도 하지 않고 null을 반환한다 — {@link #exit}도 null을 받으면 그대로 무시한다.
     */
    public static Object enter(Method method, Object[] args) {
        ValueSerializer serializer = valueSerializer;
        if (!TraceContext.isActive() || serializer == null) {
            return null;
        }
        TraceNode node = TraceContext.pushNode(method.getDeclaringClass().getName(), method.getName(),
                ArgSnapshotBuilder.build(method, args, serializer));
        return new Frame(node, System.nanoTime(), serializer);
    }

    public static void exit(Object frame, Method method, Object returnValue, Throwable throwable) {
        if (!(frame instanceof Frame)) {
            return;
        }
        Frame f = (Frame) frame;
        if (throwable != null) {
            f.node.setException(ExceptionSnapshot.from(throwable, MAX_STACK_LINES));
        } else {
            f.node.setReturnValue(f.valueSerializer.serialize(returnValue, method.getGenericReturnType()));
        }
        f.node.setDurationMs((System.nanoTime() - f.startNanos) / 1_000_000);
        TraceContext.popNode();
    }

    /** Java 8 호환을 위해 record 대신 일반 클래스로 작성(core는 Java 8 타깃). */
    private static final class Frame {
        private final TraceNode node;
        private final long startNanos;
        private final ValueSerializer valueSerializer;

        private Frame(TraceNode node, long startNanos, ValueSerializer valueSerializer) {
            this.node = node;
            this.startNanos = startNanos;
            this.valueSerializer = valueSerializer;
        }
    }
}
