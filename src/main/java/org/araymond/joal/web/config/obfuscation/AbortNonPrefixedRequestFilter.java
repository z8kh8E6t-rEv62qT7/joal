package org.araymond.joal.web.config.obfuscation;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.araymond.joal.web.config.WebUiSettings;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Reject paths outside the configured prefix without interrupting a reusable server thread. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AbortNonPrefixedRequestFilter implements Filter {
    private final WebUiSettings settings;

    public AbortNonPrefixedRequestFilter(final WebUiSettings settings) {
        this.settings = settings;
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        if (!settings.permitsPath(((HttpServletRequest) request).getRequestURI())) {
            ((HttpServletResponse) response).setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        chain.doFilter(request, response);
    }
}
