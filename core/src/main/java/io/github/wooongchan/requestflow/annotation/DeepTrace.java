package io.github.wooongchan.requestflow.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 이 애노테이션이 붙은 클래스는 빈 경계 호출뿐 아니라 클래스 내부의 self-invocation
 * ({@code this.method()}) 호출까지 계측 대상이 된다.
 *
 * <p>기본 계측(Spring AOP 프록시)은 프록시를 거치지 않는 self-invocation을 구조적으로 잡을 수 없다.
 * 이 애노테이션이 붙은 클래스는 대신 런타임에 ByteBuddy로 클래스 바이트코드 자체를 재정의해서,
 * private 메서드를 포함한 모든 인스턴스 메서드 호출을 계측한다 — 그래서 일반 프록시 기반 계측
 * 대상에서는 제외된다({@link io.github.wooongchan.requestflow.aop.RequestFlowPointcut} 참고).
 *
 * <p>대상 클래스는 {@code request-flow.base-packages} 안에 있어야 스캔되어 계측이 적용된다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DeepTrace {
}
