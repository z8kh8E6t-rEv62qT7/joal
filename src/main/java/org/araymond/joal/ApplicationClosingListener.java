package org.araymond.joal;

import lombok.extern.slf4j.Slf4j;
import org.araymond.joal.core.SeedManager;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;

/**
 * Created by raymo on 08/07/2017.
 */
@Profile("!test")
@Component
@Slf4j
public class ApplicationClosingListener implements ApplicationListener<ContextClosedEvent> {
    private final SeedManager manager;

    @Inject
    public ApplicationClosingListener(final SeedManager manager) {
        this.manager = manager;
    }

    @Override
    public void onApplicationEvent(final ContextClosedEvent event) {
        log.info("Gracefully shutting down application...");
        manager.stop();
        manager.tearDown();
        log.info("JOAL gracefully shut down");

        // Spring Boot owns logging shutdown, after the other beans have been destroyed.
    }

}
