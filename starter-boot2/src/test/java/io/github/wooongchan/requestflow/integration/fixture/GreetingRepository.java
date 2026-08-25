package io.github.wooongchan.requestflow.integration.fixture;

import org.springframework.stereotype.Repository;

@Repository
public class GreetingRepository {

    public String findMessage(Long id) {
        return "hello-" + id;
    }
}
