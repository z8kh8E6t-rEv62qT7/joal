package org.araymond.joal.web.config;

import org.araymond.joal.TestConstant;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = WebMvcConfigurationWebAppTest.WebApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.main.web-environment=true", "spring.web.resources.add-mappings=false",
                "joal.ui.path.prefix=" + TestConstant.UI_PATH_PREFIX, "joal.ui.secret-token=test-token"})
class WebMvcConfigurationWebAppTest {
    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = {SecurityAutoConfiguration.class, ErrorMvcAutoConfiguration.class})
    @Import({WebMvcConfiguration.class, WebUiSettings.class})
    static class WebApp { }

    @Autowired
    private TestRestTemplate http;

    @Test
    void servesUiOnlyAtConfiguredPrefix() {
        assertThat(http.getForEntity("/" + TestConstant.UI_PATH_PREFIX + "/ui/", String.class)
                .getBody()).contains("<html");
        assertThat(http.getForEntity("/ui/", String.class).getStatusCode().value()).isEqualTo(404);
        assertThat(http.getForEntity("/" + TestConstant.UI_PATH_PREFIX + "suffix/ui/", String.class)
                .getStatusCode().value()).isEqualTo(404);
    }
}
