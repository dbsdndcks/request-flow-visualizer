package io.github.wooongchan.requestflow.agent;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

import java.lang.reflect.Method;

/**
 * ByteBuddy가 {@code @DeepTrace} 클래스의 메서드 본문에 직접 위빙하는 계측 코드.
 * 인라인되는 코드이므로 상태(TraceContext/ValueSerializer)는 전부 {@link DeepTraceRuntime}
 * (정적 브리지)을 통해서만 접근한다 — Advice 클래스 자체는 인스턴스 필드를 가질 수 없다.
 */
public final class DeepTraceAdvice {

    private DeepTraceAdvice() {
    }

    @Advice.OnMethodEnter
    public static Object enter(@Advice.Origin Method method, @Advice.AllArguments Object[] args) {
        return DeepTraceRuntime.enter(method, args);
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(@Advice.Enter Object frame,
                             @Advice.Origin Method method,
                             @Advice.Return(typing = Assigner.Typing.DYNAMIC) Object returnValue,
                             @Advice.Thrown Throwable throwable) {
        DeepTraceRuntime.exit(frame, method, returnValue, throwable);
    }
}
