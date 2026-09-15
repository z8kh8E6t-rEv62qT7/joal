package org.araymond.joal.web.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebSocketAuthorizationSecurityConfigTest {
    @Test
    void requiresAuthenticatedMessagesWithoutRequiringCsrfHeaders() {
        final var registration = new ChannelRegistration() {
            @Override
            public java.util.List<org.springframework.messaging.support.ChannelInterceptor> getInterceptors() {
                return super.getInterceptors();
            }
        };
        new WebSocketAuthorizationSecurityConfig().configureClientInboundChannel(registration);
        final var channel = new ExecutorSubscribableChannel();
        registration.getInterceptors().forEach(channel::addInterceptor);
        channel.subscribe(message -> { });
        try {
            final var anonymous = MessageBuilder.withPayload("test").build();
            assertThatThrownBy(() -> channel.send(anonymous))
                    .hasRootCauseInstanceOf(org.springframework.security.access.AccessDeniedException.class);
            final var user = new UsernamePasswordAuthenticationToken("user", null,
                    AuthorityUtils.createAuthorityList("USER"));
            final var authenticated = MessageBuilder.withPayload("test")
                    .setHeader(SimpMessageHeaderAccessor.USER_HEADER, user).build();
            assertThatCode(() -> channel.send(authenticated)).doesNotThrowAnyException();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
