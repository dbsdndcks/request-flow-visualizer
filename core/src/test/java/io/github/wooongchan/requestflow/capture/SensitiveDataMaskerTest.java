package io.github.wooongchan.requestflow.capture;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataMaskerTest {

    private final SensitiveDataMasker masker = new SensitiveDataMasker(Arrays.asList("password", "token", "ssn"));

    @Test
    void matchesFieldNamesCaseInsensitivelyAndPartially() {
        assertThat(masker.isSensitive("password")).isTrue();
        assertThat(masker.isSensitive("userPassword")).isTrue();
        assertThat(masker.isSensitive("accessToken")).isTrue();
        assertThat(masker.isSensitive("ssnNumber")).isTrue();
    }

    @Test
    void doesNotMatchUnrelatedFieldNames() {
        assertThat(masker.isSensitive("username")).isFalse();
        assertThat(masker.isSensitive("id")).isFalse();
    }
}
