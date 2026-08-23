package io.github.wooongchan.requestflow.autoconfigure;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.List;

/**
 * request-flow.base-packages가 하나 이상 설정된 경우에만 계측용 Advisor를 등록하기 위한 조건.
 * 비어있는 채로 AspectJ 포인트컷 표현식을 만들면 문법 오류가 나므로 아예 빈 등록을 막는다.
 */
class BasePackagesConfiguredCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        List<String> basePackages = Binder.get(context.getEnvironment())
                .bind("request-flow.base-packages", Bindable.listOf(String.class))
                .orElse(List.of());
        return !basePackages.isEmpty();
    }
}
