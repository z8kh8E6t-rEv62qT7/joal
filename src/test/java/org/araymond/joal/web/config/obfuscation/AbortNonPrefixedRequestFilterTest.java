package org.araymond.joal.web.config.obfuscation;

import org.araymond.joal.TestConstant;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@SpringBootTest(
        classes = {
                org.araymond.joal.web.config.WebUiSettings.class,
                AbortNonPrefixedRequestFilter.class,
                org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration.class,
                org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration.class,
                org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration.class,
                org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration.class,
                org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration.class,
                org.springframework.boot.autoconfigure.web.servlet.HttpEncodingAutoConfiguration.class,
                org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration.class,
                org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration.class
        },
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.main.web-environment=true",
                "joal.ui.secret-token=test-token",
                "joal.ui.path.prefix=" + TestConstant.UI_PATH_PREFIX
        }
)
@Import({ AbortNonPrefixedRequestFilterTest.UnprefixedController.class, AbortNonPrefixedRequestFilterTest.PrefixedController.class })
public class AbortNonPrefixedRequestFilterTest {
    @LocalServerPort
    private int port;

    @Inject
    private TestRestTemplate restTemplate;

    @RestController
    public static class UnprefixedController {
        @RequestMapping(path = "/hello", method = RequestMethod.GET)
        public String hello() {
            return "this should not been reached :)";
        }
    }

    @RestController
    public static class PrefixedController {
        @RequestMapping(path = "/" + TestConstant.UI_PATH_PREFIX + "/hello", method = RequestMethod.GET)
        public String hello() {
            return "hello prefixed";
        }
    }

    @Test
    public void shouldRejectUnprefixedRequest() {
            final ResponseEntity<String> response = this.restTemplate.getForEntity(
                    "http://localhost:" + port + "/hello",
                    String.class
            );
            assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    public void shouldHaveResponseFromPrefixedRequest() {
        final ResponseEntity<String> response = this.restTemplate.getForEntity(
                "http://localhost:" + port + "/" + TestConstant.UI_PATH_PREFIX + "/hello",
                String.class
        );

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("hello prefixed");
    }
}
