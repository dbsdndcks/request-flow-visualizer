package io.github.wooongchan.requestflow.aop;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.StaticMethodMatcher;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * base package 아래의 계측 대상 빈을 찾는 포인트컷.
 *
 * <p>대상의 "런타임 클래스"뿐 아니라 "구현한 인터페이스"까지 본다. Spring Data 리포지토리 같은
 * 인터페이스 기반 빈은 실제 구현체가 런타임에 JDK 동적 프록시(jdk.proxy2.$ProxyNN 등)로 생성되어
 * 클래스 자체는 base package 밖에 있지만, 사용자가 작성한 리포지토리 인터페이스(@Repository)는
 * base package 안에 있다 — 그 인터페이스 쪽을 봐야 잡을 수 있다.
 *
 * <p>스테레오타입 판정은 {@code @Component} 하나만 확인한다. {@code @Controller}/{@code @RestController}/
 * {@code @Service}/{@code @Repository}는 모두 메타 애노테이션으로 {@code @Component}를 달고 있고,
 * {@link AnnotatedElementUtils}가 메타 애노테이션까지 재귀적으로 찾아주기 때문에 이걸로 충분하다.
 */
public class RequestFlowPointcut implements Pointcut {

    private static final Set<Class<?>> UNSAFE_TO_PROXY_TYPES = Set.of(
            jakarta.servlet.Filter.class,
            HandlerInterceptor.class,
            WebMvcConfigurer.class,
            ApplicationListener.class,
            BeanPostProcessor.class,
            FactoryBean.class
    );

    private final List<String> basePackages;
    private final ClassFilter classFilter = this::matchesClass;
    private final MethodMatcher methodMatcher = new StaticMethodMatcher() {
        @Override
        public boolean matches(Method method, Class<?> targetClass) {
            return method.getDeclaringClass() != Object.class;
        }
    };

    public RequestFlowPointcut(List<String> basePackages) {
        this.basePackages = basePackages;
    }

    @Override
    public ClassFilter getClassFilter() {
        return classFilter;
    }

    @Override
    public MethodMatcher getMethodMatcher() {
        return methodMatcher;
    }

    private boolean matchesClass(Class<?> clazz) {
        // JDK 동적 프록시 클래스는 항상 final이지만 Spring이 알아서 JDK 프록시로 다시 감싸므로 안전하다.
        // 그 외의 final 클래스(record 등)는 CGLIB 서브클래싱이 불가능해 프록시 생성 자체가 실패한다.
        if (Modifier.isFinal(clazz.getModifiers()) && !Proxy.isProxyClass(clazz)) {
            return false;
        }
        if (isUnsafeToProxy(clazz)) {
            return false;
        }
        if (isInBasePackageAndAnnotated(clazz)) {
            return true;
        }
        return matchesAnyInterface(clazz, new HashSet<>());
    }

    private boolean matchesAnyInterface(Class<?> clazz, Set<Class<?>> visited) {
        for (Class<?> iface : clazz.getInterfaces()) {
            if (!visited.add(iface)) {
                continue;
            }
            if (isInBasePackageAndAnnotated(iface) || matchesAnyInterface(iface, visited)) {
                return true;
            }
        }
        Class<?> superclass = clazz.getSuperclass();
        return superclass != null && superclass != Object.class && matchesAnyInterface(superclass, visited);
    }

    private boolean isInBasePackageAndAnnotated(Class<?> type) {
        return isInBasePackage(type) && AnnotatedElementUtils.hasAnnotation(type, Component.class);
    }

    private boolean isInBasePackage(Class<?> type) {
        String packageName = type.getPackageName();
        for (String basePackage : basePackages) {
            if (packageName.equals(basePackage) || packageName.startsWith(basePackage + ".")) {
                return true;
            }
        }
        return false;
    }

    private boolean isUnsafeToProxy(Class<?> clazz) {
        for (Class<?> unsafeType : UNSAFE_TO_PROXY_TYPES) {
            if (unsafeType.isAssignableFrom(clazz)) {
                return true;
            }
        }
        return false;
    }
}
