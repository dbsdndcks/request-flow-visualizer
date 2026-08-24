package io.github.wooongchan.requestflow.capture;

import io.github.wooongchan.requestflow.model.ArgSnapshot;
import io.github.wooongchan.requestflow.model.ValueSnapshot;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * 메서드 호출의 인자 목록을 {@link ArgSnapshot} 리스트로 변환한다.
 * 프록시 기반 계측(TraceMethodInterceptor)과 바이트코드 계측(DeepTraceRuntime) 양쪽에서 공유한다.
 */
public final class ArgSnapshotBuilder {

    private ArgSnapshotBuilder() {
    }

    public static List<ArgSnapshot> build(Method method, Object[] rawArgs, ValueSerializer valueSerializer) {
        if (rawArgs == null || rawArgs.length == 0) {
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
