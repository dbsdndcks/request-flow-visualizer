package io.github.wooongchan.requestflow.integration.fixture;

import org.springframework.stereotype.Repository;

/**
 * Spring Data 리포지토리처럼 "인터페이스만 사용자가 작성하고, 실제 구현체는 프레임워크가
 * 런타임에 JDK 동적 프록시로 생성"하는 패턴을 재현하기 위한 인터페이스.
 * 구현체는 {@link EchoRepositoryConfig}에서 java.lang.reflect.Proxy로 직접 만든다.
 */
@Repository
public interface EchoRepository {

    String echo(String value);
}
