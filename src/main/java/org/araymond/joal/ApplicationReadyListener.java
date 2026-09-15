package org.araymond.joal;

import lombok.extern.slf4j.Slf4j;
import org.araymond.joal.core.SeedManager;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;

/**
 * Created by raymo on 08/07/2017.
 */
@Profile("!test")
@Component
@Slf4j
public class ApplicationReadyListener implements ApplicationListener<ApplicationReadyEvent> {
    private final SeedManager manager;

    @Inject
    public ApplicationReadyListener(final SeedManager manager) {
        this.manager = manager;
    }

    @Override
    public void onApplicationEvent(final ApplicationReadyEvent event) {
        try {
            manager.init();
            manager.startSeeding();
            log.info("JOAL is ready");
        } catch (final Exception e) {
            // Spring closes the context and reports a nonzero exit when initialization fails.
            throw new IllegalStateException("JOAL failed to initialize", e);
        }
    }

}
