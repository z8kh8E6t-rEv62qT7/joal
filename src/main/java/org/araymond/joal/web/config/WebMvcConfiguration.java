package org.araymond.joal.web.config;

import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
public class WebMvcConfiguration implements WebMvcConfigurer,
        WebServerFactoryCustomizer<ConfigurableServletWebServerFactory>, Ordered {
    private final WebUiSettings settings;

    public WebMvcConfiguration(final WebUiSettings settings) {
        this.settings = settings;
    }

    @Override
    public int getOrder() {
        // Apply after Boot's server.port customizer so the disabled UI cannot open a listener.
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void customize(final ConfigurableServletWebServerFactory factory) {
        if (!settings.enabled()) {
            factory.setPort(-1);
        }
    }

    @Override
    public void addResourceHandlers(final ResourceHandlerRegistry registry) {
        if (settings.enabled()) {
            registry.addResourceHandler(settings.endpoint() + "/ui/**")
                    .addResourceLocations("classpath:/public/");
        }
    }

    @Override
    public void addViewControllers(final ViewControllerRegistry registry) {
        if (settings.enabled()) {
            final String ui = settings.endpoint() + "/ui";
            registry.addRedirectViewController(ui, ui + "/").setKeepQueryParams(true);
            registry.addViewController(ui + "/").setViewName("forward:" + ui + "/index.html");
        }
    }
}
