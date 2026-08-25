package io.github.wooongchan.requestflow.integration.fixture;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deep")
public class DeepTraceController {

    private final DeepTracedService deepTracedService;

    public DeepTraceController(DeepTracedService deepTracedService) {
        this.deepTracedService = deepTracedService;
    }

    @GetMapping("/{input}")
    public String process(@PathVariable String input) {
        return deepTracedService.process(input);
    }
}
