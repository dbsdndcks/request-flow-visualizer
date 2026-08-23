package io.github.wooongchan.requestflow.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 사용자 빈(bean)의 메서드 호출 하나를 나타내는 트리 노드.
 * AOP 인터셉터가 호출 시작 시 생성하고, 종료 시점에 returnValue/exception/durationMs를 채운다.
 */
public final class TraceNode {

    private final String className;
    private final String methodName;
    private final List<ArgSnapshot> args;
    private final List<TraceNode> children = new ArrayList<>();

    private ValueSnapshot returnValue;
    private ExceptionSnapshot exception;
    private long durationMs;

    public TraceNode(String className, String methodName, List<ArgSnapshot> args) {
        this.className = className;
        this.methodName = methodName;
        this.args = args;
    }

    public void addChild(TraceNode child) {
        children.add(child);
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public List<ArgSnapshot> getArgs() {
        return args;
    }

    public List<TraceNode> getChildren() {
        return children;
    }

    public ValueSnapshot getReturnValue() {
        return returnValue;
    }

    public void setReturnValue(ValueSnapshot returnValue) {
        this.returnValue = returnValue;
    }

    public ExceptionSnapshot getException() {
        return exception;
    }

    public void setException(ExceptionSnapshot exception) {
        this.exception = exception;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }
}
