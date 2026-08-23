package io.github.wooongchan.requestflow.aop;

import io.github.wooongchan.requestflow.model.TraceNode;
import io.github.wooongchan.requestflow.model.TraceRecord;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 요청 처리 스레드에 바인딩된 콜스택을 보관한다.
 * RequestFlowTraceFilter가 요청 시작/종료 시점에 생명주기를 관리하고,
 * TraceMethodInterceptor가 빈 호출마다 push/pop 한다.
 *
 * 주의: {@code @Async}/WebFlux처럼 스레드가 바뀌는 실행 경로에서는 콜스택이 전파되지 않는다(알려진 한계).
 */
public final class TraceContext {

    private static final ThreadLocal<Deque<TraceNode>> STACK = new ThreadLocal<>();
    private static final ThreadLocal<TraceRecord> RECORD = new ThreadLocal<>();

    private TraceContext() {
    }

    public static void startRequest(TraceRecord record) {
        RECORD.set(record);
        STACK.set(new ArrayDeque<>());
    }

    public static boolean isActive() {
        return RECORD.get() != null;
    }

    public static TraceNode pushNode(String className, String methodName,
                                      java.util.List<io.github.wooongchan.requestflow.model.ArgSnapshot> args) {
        TraceNode node = new TraceNode(className, methodName, args);
        Deque<TraceNode> stack = STACK.get();
        if (stack.isEmpty()) {
            // 스택이 비어있는데 또 push가 들어오는 경우 = 원래 호출 트리가 끝난 뒤에 별도로 실행된
            // 최상위 호출이다 (예: @RestControllerAdvice 예외 핸들러가 컨트롤러 예외를 처리하는 경우).
            // 기존 루트를 덮어쓰면 원래 호출(요청 DTO 등)이 통째로 사라지므로 별도 루트로 추가한다.
            RECORD.get().addRoot(node);
        } else {
            stack.peek().addChild(node);
        }
        stack.push(node);
        return node;
    }

    public static void popNode() {
        Deque<TraceNode> stack = STACK.get();
        if (stack != null && !stack.isEmpty()) {
            stack.pop();
        }
    }

    public static void endRequest() {
        RECORD.remove();
        STACK.remove();
    }
}
