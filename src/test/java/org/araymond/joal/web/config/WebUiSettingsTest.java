package org.araymond.joal.web.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebUiSettingsTest {
    @Test
    void disabledUiNeedsNoCredentialsAndNeverOpensHttpPort() {
        final var settings = new WebUiSettings(false, "", "", false);
        final var server = mock(ConfigurableServletWebServerFactory.class);
        new WebMvcConfiguration(settings).customize(server);
        verify(server).setPort(-1);
        assertThat(settings.permitsPath("/")).isFalse();
    }

    @Test
    void enabledUiKeepsConfiguredPortAndRequiresSafeSettings() {
        final var settings = new WebUiSettings(true, "Secret123", "test-token", false);
        final var server = mock(ConfigurableServletWebServerFactory.class);
        new WebMvcConfiguration(settings).customize(server);
        verifyNoInteractions(server);
        assertThat(settings.permitsPath("/Secret123")).isTrue();
        assertThat(settings.permitsPath("/Secret123/ui/")).isTrue();
        assertThat(settings.permitsPath("/Secret123suffix/ui/")).isFalse();
        assertThat(settings.toString()).doesNotContain("test-token");
        for (String prefix : new String[]{"", " ", "../", "a/b", "a\\b", "a?b"}) {
            assertThatIllegalArgumentException().isThrownBy(() -> new WebUiSettings(true, prefix, "token", false));
        }
        assertThatIllegalArgumentException().isThrownBy(() -> new WebUiSettings(true, "safe", " ", false));
    }
}
