package org.araymond.joal.web.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Runtime UI settings: these must never determine the AOT bean graph. */
@Component
public record WebUiSettings(boolean enabled, String pathPrefix, String secretToken, boolean iframeEnabled) {
    public WebUiSettings(
            @Value("${spring.main.web-environment:false}") final boolean enabled,
            @Value("${joal.ui.path.prefix:}") final String pathPrefix,
            @Value("${joal.ui.secret-token:}") final String secretToken,
            @Value("${joal.iframe.enabled:false}") final boolean iframeEnabled) {
        if (enabled && !pathPrefix.matches("[A-Za-z0-9]+")) {
            throw new IllegalArgumentException("joal.ui.path.prefix must contain only letters and digits when the Web UI is enabled");
        }
        if (enabled && secretToken.isBlank()) {
            throw new IllegalArgumentException("joal.ui.secret-token must not be blank when the Web UI is enabled");
        }
        this.enabled = enabled;
        this.pathPrefix = pathPrefix;
        this.secretToken = secretToken;
        this.iframeEnabled = iframeEnabled;
    }

    public String endpoint() {
        return "/" + pathPrefix;
    }

    public boolean permitsPath(final String path) {
        return enabled && (path.equals(endpoint()) || path.startsWith(endpoint() + "/"));
    }

    @Override
    public String toString() {
        return "WebUiSettings[enabled=" + enabled + ", pathPrefix=" + pathPrefix + ", iframeEnabled=" + iframeEnabled + "]";
    }
}
