package io.github.wooongchan.requestflow.agent;

import io.github.wooongchan.requestflow.annotation.DeepTrace;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.instrument.Instrumentation;
import java.util.List;

/**
 * {@code @DeepTrace}가 붙은 클래스가 있으면 ByteBuddy 에이전트를 self-attach해서 그 클래스들의
 * 바이트코드를 재정의(self-invocation까지 계측)한다. {@code BeanFactoryPostProcessor}는 다른 빈보다
 * 먼저 생성되는 Spring 표준 확장점이라, 사용자 서비스 빈이 실제로 인스턴스화되기 전에 클래스 재정의가
 * 끝나 있는 게 보장된다 (단, {@code RedefinitionStrategy.RETRANSFORMATION}을 쓰므로 이미 로드된
 * 클래스가 있어도 안전하게 재정의된다).
 *
 * <p>self-attach나 위빙이 실패해도 앱 기동을 절대 막지 않는다 — 경고 로그만 남기고 deep-trace 없이
 * 정상 기동한다. JDK 배포판/실행 환경에 따라 self-attach가 막혀 있을 수 있기 때문이다.
 */
public class DeepTraceAgentInstaller implements BeanFactoryPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(DeepTraceAgentInstaller.class);

    private final List<String> basePackages;

    public DeepTraceAgentInstaller(List<String> basePackages) {
        this.basePackages = basePackages;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (!hasAnyDeepTraceCandidate()) {
            return;
        }
        try {
            Instrumentation instrumentation = ByteBuddyAgent.install();
            installAdvice(instrumentation);
            log.info("[request-flow] @DeepTrace 계측이 활성화되었습니다.");
        } catch (Throwable t) {
            log.warn("[request-flow] @DeepTrace 계측 설치에 실패해 내부 호출(self-invocation) 계측 없이 "
                    + "계속 진행합니다 (빈 경계 계측은 정상 동작). 원인: {}", t.toString());
        }
    }

    private boolean hasAnyDeepTraceCandidate() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(DeepTrace.class));
        for (String basePackage : basePackages) {
            if (!scanner.findCandidateComponents(basePackage.trim()).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void installAdvice(Instrumentation instrumentation) {
        new AgentBuilder.Default()
                .disableClassFormatChanges()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                .type(ElementMatchers.isAnnotatedWith(DeepTrace.class))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                        builder.visit(Advice.to(DeepTraceAdvice.class).on(instrumentableMethods())))
                .installOn(instrumentation);
    }

    /** 생성자/static/synthetic·bridge 메서드와 흔히 자동 생성되는 equals/hashCode/toString은 제외한다. */
    private static ElementMatcher.Junction<MethodDescription> instrumentableMethods() {
        ElementMatcher.Junction<MethodDescription> boilerplate =
                ElementMatchers.<MethodDescription>named("equals").and(ElementMatchers.takesArguments(Object.class))
                        .or(ElementMatchers.named("hashCode").and(ElementMatchers.takesArguments(0)))
                        .or(ElementMatchers.named("toString").and(ElementMatchers.takesArguments(0)));

        return ElementMatchers.<MethodDescription>isMethod()
                .and(ElementMatchers.not(ElementMatchers.isStatic()))
                .and(ElementMatchers.not(ElementMatchers.isAbstract()))
                .and(ElementMatchers.not(ElementMatchers.isSynthetic()))
                .and(ElementMatchers.not(ElementMatchers.isBridge()))
                .and(ElementMatchers.not(boilerplate));
    }
}
