package io.github.wooongchan.requestflow.capture;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.wooongchan.requestflow.model.ValueSnapshot;
import io.github.wooongchan.requestflow.util.ReflectiveTypeMatcher;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.Socket;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 메서드 인자/리턴값을 JSON 트리로 안전하게 변환한다.
 * 순환 참조, 대용량 컬렉션, 스트림/커넥션류 객체를 방어적으로 처리한다.
 */
public class ValueSerializer {

    // core는 javax.servlet/jakarta.servlet 어느 쪽에도 컴파일 의존성이 없어서, 클래스 리터럴 대신
    // 이름으로 매칭한다(ReflectiveTypeMatcher) — 두 네임스페이스 모두 인식된다.
    private static final Set<String> UNSERIALIZABLE_INTERFACE_NAMES = new HashSet<>(Arrays.asList(
            "javax.servlet.http.HttpServletRequest", "jakarta.servlet.http.HttpServletRequest",
            "javax.servlet.http.HttpServletResponse", "jakarta.servlet.http.HttpServletResponse",
            "javax.servlet.http.HttpSession", "jakarta.servlet.http.HttpSession"));

    private final ObjectMapper objectMapper;
    private final int maxCollectionSize;
    private final int maxValueLength;

    public ValueSerializer(SensitiveDataMasker masker, int maxCollectionSize, int maxValueLength) {
        this.maxCollectionSize = maxCollectionSize;
        this.maxValueLength = maxValueLength;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.registerModule(new Jdk8Module());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        masker.applyTo(objectMapper);
    }

    public ValueSnapshot serialize(Object value, Type declaredType) {
        String typeLabel = TypeNames.format(declaredType);

        if (value == null) {
            return new ValueSnapshot(typeLabel, NullNode.getInstance(), false);
        }
        if (isKnownUnserializable(value)) {
            return new ValueSnapshot(typeLabel,
                    TextNode.valueOf("<unserializable: " + value.getClass().getName() + ">"), false);
        }

        Object toSerialize = value;
        boolean collectionTruncated = false;
        if (value instanceof Collection<?> && ((Collection<?>) value).size() > maxCollectionSize) {
            Collection<?> collection = (Collection<?>) value;
            toSerialize = collection.stream().limit(maxCollectionSize).collect(Collectors.toList());
            collectionTruncated = true;
        } else if (value instanceof Map<?, ?> && ((Map<?, ?>) value).size() > maxCollectionSize) {
            Map<?, ?> map = (Map<?, ?>) value;
            Map<Object, Object> limited = new LinkedHashMap<>();
            int i = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (i++ >= maxCollectionSize) {
                    break;
                }
                limited.put(entry.getKey(), entry.getValue());
            }
            toSerialize = limited;
            collectionTruncated = true;
        }

        JsonNode node;
        try {
            node = objectMapper.valueToTree(toSerialize);
        } catch (Throwable t) {
            // 순환 참조(JsonMappingException) 또는 드문 StackOverflowError까지 방어적으로 폴백
            String fallback = value.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(value));
            return new ValueSnapshot(typeLabel, TextNode.valueOf(fallback), true);
        }

        String asString = node.toString();
        boolean lengthTruncated = false;
        if (asString.length() > maxValueLength) {
            node = TextNode.valueOf(asString.substring(0, maxValueLength) + "...(truncated)");
            lengthTruncated = true;
        }
        return new ValueSnapshot(typeLabel, node, collectionTruncated || lengthTruncated);
    }

    private boolean isKnownUnserializable(Object value) {
        if (value instanceof InputStream
                || value instanceof OutputStream
                || value instanceof Reader
                || value instanceof Writer
                || value instanceof Connection
                || value instanceof Socket
                || value instanceof MultipartFile) {
            return true;
        }
        return ReflectiveTypeMatcher.implementsAnyNamed(value.getClass(), UNSERIALIZABLE_INTERFACE_NAMES);
    }
}
