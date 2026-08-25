package io.github.wooongchan.requestflow.integration.fixture;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * healthy-plate의 GlobalExceptionHandler를 재현하는 픽스처.
 * 컨트롤러가 예외를 던지면 Spring MVC가 원래 호출 스택과 무관하게 이 빈을 별도로 호출하는데,
 * 그 시점엔 TraceContext의 콜스택이 이미 비어있어 "새 root"로 취급돼야 한다
 * (기존 root를 덮어쓰면 원래 컨트롤러 호출의 인자/예외 정보가 사라진다).
 */
@RestControllerAdvice
public class FixtureExceptionHandler {

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(500).body("handled: " + e.getMessage());
    }
}
