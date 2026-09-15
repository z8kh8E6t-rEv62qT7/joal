package org.araymond.joal.web.config.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.security.messaging.access.intercept.AuthorizationChannelInterceptor;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration(proxyBeanMethods = false)
public class WebSocketAuthorizationSecurityConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureClientInboundChannel(final ChannelRegistration registration) {
        final var authorization = MessageMatcherDelegatingAuthorizationManager.builder()
                .anyMessage().authenticated().build();
        // Token authentication runs first. Preserve the STOMP protocol without CSRF headers.
        registration.interceptors(new SecurityContextChannelInterceptor(),
                new AuthorizationChannelInterceptor(authorization));
    }
}
