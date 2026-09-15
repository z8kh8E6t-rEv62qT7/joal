package org.araymond.joal.web.config.security;

import org.araymond.joal.TestConstant;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import jakarta.inject.Inject;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = {
                org.araymond.joal.web.config.WebUiSettings.class,
                WebSecurityConfig.class,
                org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration.class,
                org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration.class,
                org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration.class,
                org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration.class,
                org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration.class,
                org.springframework.boot.autoconfigure.web.servlet.HttpEncodingAutoConfiguration.class,
                org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration.class,
                org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
                org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration.class
        },
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.main.web-environment=true",
                "joal.ui.path.prefix=" + TestConstant.UI_PATH_PREFIX,
                "joal.ui.secret-token=" + TestConstant.UI_SECRET_TOKEN
        }
)
@Import({WebSecurityConfigWebAppTest.TestWebUiController.class, WebSecurityConfigWebAppTest.TestWebSocketConfig.class})
public class WebSecurityConfigWebAppTest {

    @LocalServerPort
    private int port;

    @Inject
    private TestRestTemplate restTemplate;

    @RestController
    public static class TestWebUiController {
        @RequestMapping(path = TestConstant.UI_PATH_PREFIX + "/ui/", method = RequestMethod.GET)
        public String mockedCtrl() {
            return "";
        }
    }

    @TestConfiguration
    @EnableWebSocketMessageBroker
    public static class TestWebSocketConfig implements WebSocketMessageBrokerConfigurer {
        @Override
        public void registerStompEndpoints(final StompEndpointRegistry registry) {
            registry.addEndpoint(TestConstant.UI_PATH_PREFIX).setAllowedOrigins("*");
        }
    }

    @Test
    public void shouldForbidNonPrefixedUri() {
        assertThat(this.restTemplate.getForEntity("http://localhost:" + port + "", String.class).getStatusCodeValue()).isEqualTo(403);
        assertThat(this.restTemplate.getForEntity("http://localhost:" + port + "/ui/", String.class).getStatusCodeValue()).isEqualTo(403);
        assertThat(this.restTemplate.getForEntity("http://localhost:" + port + "/bla", String.class).getStatusCodeValue()).isEqualTo(403);
    }

    @Test
    public void shouldPermitOnPrefixedUriForWebsocketHandshakeEndpoint() throws InterruptedException, ExecutionException, TimeoutException {
        final WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());

        final StompSession stompSession = stompClient.connectAsync("ws://localhost:" + port + "/" + TestConstant.UI_PATH_PREFIX, new StompSessionHandlerAdapter() {
        }).get(10, TimeUnit.SECONDS);

        assertThat(stompSession.isConnected()).isTrue();
    }

    @Test
    public void shouldPermitPrefixedUriOnWebUiEndpoint() {
        assertThat(this.restTemplate.getForEntity(
                "http://localhost:" + port + "/" + TestConstant.UI_PATH_PREFIX + "/ui/",
                String.class
        ).getStatusCodeValue()).isEqualTo(200);
    }

}


