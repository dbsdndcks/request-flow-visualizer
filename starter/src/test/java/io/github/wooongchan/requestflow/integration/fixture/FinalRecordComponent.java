package io.github.wooongchan.requestflow.integration.fixture;

import org.springframework.stereotype.Component;

/**
 * JwtProperties 같은 "record + @Component" 조합을 재현하는 픽스처.
 * record는 암묵적으로 final이라 CGLIB 서브클래싱이 불가능하다 — 계측 대상 포인트컷이 이런 빈까지
 * 잡으면 Spring AOP 프록시 생성 자체가 실패해서 앱 기동이 깨진다 (실제 운영 프로젝트에서 발견된 회귀).
 */
@Component
public record FinalRecordComponent(String value) {

    public FinalRecordComponent() {
        this("default");
    }

    public String describe() {
        return "value=" + value;
    }
}
