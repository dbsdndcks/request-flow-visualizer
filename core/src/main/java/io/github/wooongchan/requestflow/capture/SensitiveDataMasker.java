package io.github.wooongchan.requestflow.capture;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 필드명이 민감정보 패턴(password, token 등)에 부분일치하면 값을 마스킹 처리한다.
 * 직렬화 트리 전체에 재귀적으로 적용되도록 Jackson BeanSerializerModifier로 구현.
 */
public class SensitiveDataMasker {

    private static final String MASK = "***MASKED***";

    private final List<String> lowerCasePatterns;

    public SensitiveDataMasker(List<String> patterns) {
        this.lowerCasePatterns = patterns.stream()
                .map(p -> p.toLowerCase(Locale.ROOT))
                .collect(Collectors.toList());
    }

    public boolean isSensitive(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        String lower = fieldName.toLowerCase(Locale.ROOT);
        return lowerCasePatterns.stream().anyMatch(lower::contains);
    }

    public void applyTo(ObjectMapper mapper) {
        mapper.setSerializerFactory(mapper.getSerializerFactory().withSerializerModifier(new MaskingModifier()));
    }

    private final class MaskingModifier extends BeanSerializerModifier {
        @Override
        public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc,
                                                           List<BeanPropertyWriter> beanProperties) {
            for (BeanPropertyWriter writer : beanProperties) {
                if (isSensitive(writer.getName())) {
                    writer.assignSerializer(new MaskingSerializer());
                }
            }
            return beanProperties;
        }
    }

    private static final class MaskingSerializer extends JsonSerializer<Object> {
        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws java.io.IOException {
            gen.writeString(MASK);
        }
    }
}
