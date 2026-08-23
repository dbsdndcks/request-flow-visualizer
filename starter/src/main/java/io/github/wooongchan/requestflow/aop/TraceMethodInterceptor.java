package io.github.wooongchan.requestflow.aop;

import io.github.wooongchan.requestflow.capture.ValueSerializer;
import io.github.wooongchan.requestflow.model.ArgSnapshot;
import io.github.wooongchan.requestflow.model.ExceptionSnapshot;
import io.github.wooongchan.requestflow.model.TraceNode;
import io.github.wooongchan.requestflow.model.ValueSnapshot;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * 계측 대상 빈 호출을 감싸는 Around 어드바이스.
 * 진행 중인 요청 트레이스가 없으면(TraceContext 비활성) 오버헤드 없이 그대로 통과시킨다.
 */
public class TraceMethodInterceptor implements MethodInterceptor {

    private static final int MAX_STACK_LINES = 5;

    private final ValueSerializer valueSerializer;

    public TraceMethodInterceptor(ValueSerializer valueSerializer) {
        this.valueSerializer = valueSerializer;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        if (!TraceContext.isActive()) {
            return invocation.proceed();
        }

        Method method = invocation.getMethod();
        String className = resolveClassName(invocation, method);

        TraceNode node = TraceContext.pushNode(className, method.getName(),
                buildArgs(method, invocation.getArguments()));
        long startNanos = System.nanoTime();
        try {
            Object result = invocation.proceed();
            node.setReturnValue(valueSerializer.serialize(result, method.getGenericReturnType()));
            return result;
        } catch (Throwable throwable) {
            node.setException(ExceptionSnapshot.from(throwable, MAX_STACK_LINES));
            throw throwable;
        } finally {
            node.setDurationMs((System.nanoTime() - startNanos) / 1_000_000);
            TraceContext.popNode();
        }
    }

    /**
     * Spring Data 리포지토리처럼 대상이 JDK 동적 프록시(jdk.proxy2.$ProxyNN 등)면 그 클래스명은
     * 아무 의미가 없으므로, 그 경우엔 실제로 호출이 선언된 인터페이스(method의 declaring class)를 쓴다.
     */
    private String resolveClassName(MethodInvocation invocation, Method method) {
        Object target = invocation.getThis();
        if (target != null) {
            Class<?> userClass = ClassUtils.getUserClass(target);
            if (!Proxy.isProxyClass(userClass)) {
                return userClass.getName();
            }
        }
        return method.getDeclaringClass().getName();
    }

    private List<ArgSnapshot> buildArgs(Method method, Object[] rawArgs) {
        if (rawArgs.length == 0) {
            return List.of();
        }
        Parameter[] parameters = method.getParameters();
        Type[] genericTypes = method.getGenericParameterTypes();
        List<ArgSnapshot> args = new ArrayList<>(rawArgs.length);
        for (int i = 0; i < rawArgs.length; i++) {
            String name = parameters[i].isNamePresent() ? parameters[i].getName() : "arg" + i;
            ValueSnapshot snapshot = valueSerializer.serialize(rawArgs[i], genericTypes[i]);
            args.add(new ArgSnapshot(name, snapshot.getType(), snapshot.getValue(), snapshot.isTruncated()));
        }
        return args;
    }
}
