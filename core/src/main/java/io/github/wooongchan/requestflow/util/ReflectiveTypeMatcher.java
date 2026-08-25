package io.github.wooongchan.requestflow.util;

import java.util.HashSet;
import java.util.Set;

/**
 * 클래스가 특정 정규화된 이름(FQCN)의 인터페이스/부모 클래스를 구현·상속하는지 리플렉션으로 확인한다.
 *
 * <p>core 모듈은 javax.servlet과 jakarta.servlet 어느 쪽에도 컴파일 의존성을 갖지 않는다.
 * 그런데 {@code HttpServletRequest}(ValueSerializer)나 {@code Filter}(RequestFlowPointcut)처럼
 * 두 네임스페이스에 이름은 같고 패키지만 다른 타입을 인식해야 하는 경우가 있어서, 실제 클래스
 * 리터럴 대신 문자열 이름으로 재귀 탐색한다.
 */
public final class ReflectiveTypeMatcher {

    private ReflectiveTypeMatcher() {
    }

    public static boolean implementsAnyNamed(Class<?> clazz, Set<String> fullyQualifiedNames) {
        return matches(clazz, fullyQualifiedNames, new HashSet<Class<?>>());
    }

    private static boolean matches(Class<?> clazz, Set<String> names, Set<Class<?>> visited) {
        if (clazz == null || clazz == Object.class || !visited.add(clazz)) {
            return false;
        }
        if (names.contains(clazz.getName())) {
            return true;
        }
        for (Class<?> iface : clazz.getInterfaces()) {
            if (matches(iface, names, visited)) {
                return true;
            }
        }
        return matches(clazz.getSuperclass(), names, visited);
    }
}
