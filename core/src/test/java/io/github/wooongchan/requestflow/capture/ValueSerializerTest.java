package io.github.wooongchan.requestflow.capture;

import io.github.wooongchan.requestflow.model.ValueSnapshot;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class ValueSerializerTest {

    private final ValueSerializer serializer =
            new ValueSerializer(new SensitiveDataMasker(Arrays.asList("password", "token")), 3, 5000);

    @Test
    void serializesSimpleValue() {
        ValueSnapshot snapshot = serializer.serialize("hello", String.class);

        assertThat(snapshot.getType()).isEqualTo("String");
        assertThat(snapshot.getValue().asText()).isEqualTo("hello");
        assertThat(snapshot.isTruncated()).isFalse();
    }

    @Test
    void truncatesCollectionsLargerThanMaxSize() {
        List<Integer> big = IntStream.range(0, 10).boxed().collect(Collectors.toList());

        ValueSnapshot snapshot = serializer.serialize(big, List.class);

        assertThat(snapshot.isTruncated()).isTrue();
        assertThat(snapshot.getValue().size()).isEqualTo(3);
    }

    @Test
    void masksSensitiveFieldsInSerializedObject() {
        ValueSnapshot snapshot = serializer.serialize(new LoginRequest("bob", "hunter2"), LoginRequest.class);

        assertThat(snapshot.getValue().get("username").asText()).isEqualTo("bob");
        assertThat(snapshot.getValue().get("password").asText()).isEqualTo("***MASKED***");
    }

    @Test
    void fallsBackGracefullyOnCircularReference() {
        Node node = new Node();
        node.self = node;

        ValueSnapshot snapshot = serializer.serialize(node, Node.class);

        assertThat(snapshot.isTruncated()).isTrue();
        assertThat(snapshot.getValue().asText()).contains("Node@");
    }

    @Test
    void marksKnownUnserializableTypesWithoutReadingThem() {
        InputStream stream = new ByteArrayInputStream("secret-bytes".getBytes());

        ValueSnapshot snapshot = serializer.serialize(stream, InputStream.class);

        assertThat(snapshot.getValue().asText()).contains("unserializable");
    }

    static class Node {
        public Node self;
    }

    static class LoginRequest {
        public final String username;
        public final String password;

        LoginRequest(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }
}
