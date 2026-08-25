package io.github.wooongchan.requestflow.integration.fixture;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Proxy;

@Configuration
public class EchoRepositoryConfig {

    @Bean
    public EchoRepository echoRepository() {
        return (EchoRepository) Proxy.newProxyInstance(
                EchoRepository.class.getClassLoader(),
                new Class<?>[]{EchoRepository.class},
                (proxy, method, args) -> {
                    if ("echo".equals(method.getName())) {
                        return "echo:" + args[0];
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }
}
