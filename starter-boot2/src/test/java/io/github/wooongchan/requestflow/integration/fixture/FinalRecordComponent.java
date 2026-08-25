package io.github.wooongchan.requestflow.integration.fixture;

import org.springframework.stereotype.Component;

/**
 * JwtProperties 같은 "final 클래스 + @Component" 조합을 재현하는 픽스처.
 * CGLIB는 final 클래스를 서브클래싱할 수 없다 — 계측 대상 포인트컷이 이런 빈까지 잡으면 Spring AOP
 * 프록시 생성 자체가 실패해서 앱 기동이 깨진다 (실제 운영 프로젝트에서 발견된 회귀).
 *
 * <p>starter-boot3 쪽 픽스처는 record(Java 16+)를 쓰는데, 여기는 Java 8 타깃이라 일반
 * final 클래스로 같은 시나리오(= final 클래스)를 재현한다.
 */
@Component
public final class FinalRecordComponent {

    private final String value;

    public FinalRecordComponent() {
        this("default");
    }

    public FinalRecordComponent(String value) {
        this.value = value;
    }

    public String describe() {
        return "value=" + value;
    }
}
