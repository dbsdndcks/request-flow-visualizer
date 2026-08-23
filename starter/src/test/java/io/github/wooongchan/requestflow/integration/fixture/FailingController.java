package io.github.wooongchan.requestflow.integration.fixture;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FailingController {

    @GetMapping("/api/fail")
    public String fail() {
        throw new IllegalStateException("boom");
    }
}
