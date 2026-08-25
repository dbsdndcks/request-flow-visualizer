package io.github.wooongchan.requestflow.integration.fixture;

import org.springframework.stereotype.Service;

@Service
public class GreetingService {

    private final GreetingRepository repository;
    private final EchoRepository echoRepository;

    public GreetingService(GreetingRepository repository, EchoRepository echoRepository) {
        this.repository = repository;
        this.echoRepository = echoRepository;
    }

    public String greet(Long id) {
        String message = repository.findMessage(id);
        return echoRepository.echo(message);
    }
}
