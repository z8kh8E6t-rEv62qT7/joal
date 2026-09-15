package org.araymond.joal.web.config.security;

import org.araymond.joal.web.config.WebUiSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Created by raymo on 29/07/2017.
 */
@EnableWebSecurity
@Configuration
public class WebSecurityConfig {
    private final WebUiSettings settings;

    public WebSecurityConfig(final WebUiSettings settings) {
        this.settings = settings;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        if (settings.iframeEnabled()) {
            http.headers(headers -> headers.frameOptions(options -> options.disable()));
        }

        return http
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> {
                    if (settings.enabled()) {
                        requests.requestMatchers(settings.endpoint(), settings.endpoint() + "/ui/**").permitAll();
                    }
                    requests.anyRequest().denyAll();
                })
                .build();
    }

    // Provide an empty UserDetailService to prevent spring from injecting a default one with a valid random password.
    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        return new InMemoryUserDetailsManager();
    }

}
