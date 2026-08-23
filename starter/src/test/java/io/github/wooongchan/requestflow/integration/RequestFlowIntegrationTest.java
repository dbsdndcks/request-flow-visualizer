package io.github.wooongchan.requestflow.integration;

import io.github.wooongchan.requestflow.integration.fixture.FinalRecordComponent;
import io.github.wooongchan.requestflow.model.TraceNode;
import io.github.wooongchan.requestflow.model.TraceRecord;
import io.github.wooongchan.requestflow.model.TraceSummary;
import io.github.wooongchan.requestflow.store.TraceRingBuffer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "request-flow.base-packages=io.github.wooongchan.requestflow.integration.fixture"
})
class RequestFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TraceRingBuffer traceRingBuffer;

    @Autowired
    private FinalRecordComponent finalRecordComponent;

    @Test
    void doesNotProxyFinalClassesLikeRecords() {
        // 컨텍스트가 뜨고, final(record) 빈 메서드도 정상 호출된다는 것 자체가
        // AOP 프록시 생성 실패 없이 넘어갔다는 뜻이다.
        assertThat(finalRecordComponent.describe()).isEqualTo("value=default");
    }

    @Test
    void capturesControllerServiceRepositoryCallTree() throws Exception {
        traceRingBuffer.clear();

        mockMvc.perform(get("/api/greet/42"))
                .andExpect(status().isOk());

        List<TraceSummary> summaries = traceRingBuffer.list(10);
        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).status()).isEqualTo(200);

        TraceRecord record = traceRingBuffer.findById(summaries.get(0).traceId()).orElseThrow();
        assertThat(record.getRoots()).hasSize(1);
        TraceNode controllerNode = record.getRoots().get(0);
        assertThat(controllerNode).isNotNull();
        assertThat(controllerNode.getClassName()).endsWith("GreetingController");
        assertThat(controllerNode.getMethodName()).isEqualTo("getGreeting");
        assertThat(controllerNode.getArgs()).hasSize(1);
        assertThat(controllerNode.getArgs().get(0).getValue().asLong()).isEqualTo(42);

        assertThat(controllerNode.getChildren()).hasSize(1);
        TraceNode serviceNode = controllerNode.getChildren().get(0);
        assertThat(serviceNode.getClassName()).endsWith("GreetingService");

        assertThat(serviceNode.getChildren()).hasSize(2);
        TraceNode repositoryNode = serviceNode.getChildren().get(0);
        assertThat(repositoryNode.getClassName()).endsWith("GreetingRepository");
        assertThat(repositoryNode.getReturnValue().getValue().asText()).isEqualTo("hello-42");

        // Spring Data 리포지토리처럼 JDK 동적 프록시로 만들어진 빈도 잡히는지,
        // 클래스명이 프록시 클래스(jdk.proxy2.$ProxyNN)가 아니라 사용자 인터페이스로 나오는지 검증.
        TraceNode echoRepositoryNode = serviceNode.getChildren().get(1);
        assertThat(echoRepositoryNode.getClassName()).isEqualTo(
                "io.github.wooongchan.requestflow.integration.fixture.EchoRepository");
        assertThat(echoRepositoryNode.getMethodName()).isEqualTo("echo");
        assertThat(echoRepositoryNode.getReturnValue().getValue().asText()).isEqualTo("echo:hello-42");
    }

    @Test
    void keepsOriginalControllerRootWhenExceptionHandlerRunsAfterStackUnwinds() throws Exception {
        traceRingBuffer.clear();

        mockMvc.perform(get("/api/fail"))
                .andExpect(status().isInternalServerError());

        List<TraceSummary> summaries = traceRingBuffer.list(10);
        assertThat(summaries).hasSize(1);

        TraceRecord record = traceRingBuffer.findById(summaries.get(0).traceId()).orElseThrow();
        assertThat(record.getRoots()).hasSize(2);

        TraceNode controllerRoot = record.getRoots().get(0);
        assertThat(controllerRoot.getClassName()).endsWith("FailingController");
        assertThat(controllerRoot.getException()).isNotNull();
        assertThat(controllerRoot.getException().getType()).endsWith("IllegalStateException");

        TraceNode handlerRoot = record.getRoots().get(1);
        assertThat(handlerRoot.getClassName()).endsWith("FixtureExceptionHandler");
        assertThat(handlerRoot.getReturnValue().getValue().get("body").asText()).isEqualTo("handled: boom");
    }
}
