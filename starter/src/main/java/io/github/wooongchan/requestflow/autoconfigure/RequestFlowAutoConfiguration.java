package io.github.wooongchan.requestflow.autoconfigure;

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
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.Ordered;

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
        return new ValueSerializer(masker, properties.getMaxCollectionSize(), properties.getMaxValueLength());
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
