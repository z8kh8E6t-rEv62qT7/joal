package org.araymond.joal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.araymond.joal.conf.JoalRuntimeHints;
import org.apache.logging.log4j.jul.Log4jBridgeHandler;

@EnableAsync
@ImportRuntimeHints(JoalRuntimeHints.class)
@SpringBootApplication(exclude = ErrorMvcAutoConfiguration.class)
public class JackOfAllTradesApplication {

    public static void main(final String[] args) {
        // Install directly so the JUL bridge is reachable in native images before Tomcat starts.
        Log4jBridgeHandler.install(true, null, true);
        SpringApplication.run(JackOfAllTradesApplication.class, args);
    }
}
