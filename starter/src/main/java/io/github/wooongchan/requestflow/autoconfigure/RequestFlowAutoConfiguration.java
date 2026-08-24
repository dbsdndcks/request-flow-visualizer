package io.github.wooongchan.requestflow.autoconfigure;

import io.github.wooongchan.requestflow.agent.DeepTraceAgentInstaller;
import io.github.wooongchan.requestflow.agent.DeepTraceRuntime;
import io.github.wooongchan.requestflow.aop.RequestFlowPointcut;
import io.github.wooongchan.requestflow.aop.TraceMethodInterceptor;
import io.github.wooongchan.requestflow.capture.SensitiveDataMasker;
import io.github.wooongchan.requestflow.capture.ValueSerializer;
import io.github.wooongchan.requestflow.store.TraceRingBuffer;
import io.github.wooongchan.requestflow.web.RequestFlowTraceFilter;
import io.github.wooongchan.requestflow.web.TraceApiController;
import io.github.wooongchan.requestflow.web.ViewerWebConfig;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.Advisor;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

import java.util.List;

@AutoConfiguration
@EnableConfigurationProperties(RequestFlowProperties.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "request-flow", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RequestFlowAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RequestFlowAutoConfiguration.class);

    private final RequestFlowProperties properties;

    public RequestFlowAutoConfiguration(RequestFlowProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void warnIfNotConfigured() {
        if (properties.getBasePackages().isEmpty()) {
            log.warn("[request-flow] base-packages가 설정되지 않아 요청 흐름 계측이 비활성화됩니다. "
                    + "application.yml에 request-flow.base-packages를 설정하세요 (예: com.mycompany).");
        }
    }

    @Bean
    public SensitiveDataMasker sensitiveDataMasker() {
        return new SensitiveDataMasker(properties.getMaskFieldPatterns());
    }

    @Bean
    public ValueSerializer valueSerializer(SensitiveDataMasker masker) {
        ValueSerializer valueSerializer =
                new ValueSerializer(masker, properties.getMaxCollectionSize(), properties.getMaxValueLength());
        // DeepTraceAdvice는 ByteBuddy가 대상 메서드 본문에 인라인하는 코드라 이 빈을 직접 주입받을 수
        // 없다 - 정적 브리지에 한 번 등록해서 참조할 수 있게 한다.
        DeepTraceRuntime.register(valueSerializer);
        return valueSerializer;
    }

    /**
     * {@code @DeepTrace} 클래스가 있으면 ByteBuddy 에이전트를 self-attach해서 self-invocation까지
     * 계측한다. BeanFactoryPostProcessor는 반드시 static 메서드여야 조기에 생성된다(다른 일반 빈보다
     * 먼저 인스턴스화되어야 사용자 서비스 빈이 뜨기 전에 클래스 재정의가 끝나 있다). 그래서 이
     * {@code @Configuration} 클래스의 인스턴스 필드(properties)를 못 쓰고, base-packages를
     * Environment에서 직접 바인딩한다({@link BasePackagesConfiguredCondition}과 동일한 방식).
     */
    @Bean
    public static BeanFactoryPostProcessor deepTraceAgentInstaller(Environment environment) {
        List<String> basePackages = Binder.get(environment)
                .bind("request-flow.base-packages", Bindable.listOf(String.class))
                .orElse(List.of());
        return new DeepTraceAgentInstaller(basePackages);
    }

    @Bean
    public TraceRingBuffer traceRingBuffer() {
        return new TraceRingBuffer(properties.getMaxTraces());
    }

    @Bean
    @Conditional(BasePackagesConfiguredCondition.class)
    public Advisor requestFlowTraceAdvisor(ValueSerializer valueSerializer) {
        Pointcut pointcut = new RequestFlowPointcut(properties.getBasePackages().stream().map(String::trim).toList());
        TraceMethodInterceptor advice = new TraceMethodInterceptor(valueSerializer);
        return new DefaultPointcutAdvisor(pointcut, advice);
    }

    @Bean
    public FilterRegistrationBean<RequestFlowTraceFilter> requestFlowTraceFilter(TraceRingBuffer store) {
        FilterRegistrationBean<RequestFlowTraceFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RequestFlowTraceFilter(store, properties.getViewerPath()));
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setName("requestFlowTraceFilter");
        return registration;
    }

    @Bean
    public TraceApiController traceApiController(TraceRingBuffer store) {
        return new TraceApiController(store);
    }

    @Bean
    public ViewerWebConfig requestFlowViewerWebConfig() {
        return new ViewerWebConfig(properties.getViewerPath());
    }
}
