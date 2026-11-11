package vn.newmen.store.config;

import jakarta.servlet.Servlet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "spring.h2.console", name = "enabled", havingValue = "true")
public class H2ConsoleConfiguration {
    @Bean
    ServletRegistrationBean<Servlet> h2ConsoleServlet(@Value("${spring.h2.console.path:/h2-console}") String configuredPath) {
        String path = configuredPath.startsWith("/") ? configuredPath : "/" + configuredPath;
        while (path.endsWith("/")) path = path.substring(0, path.length() - 1);
        try {
            Class<?> consoleType = Class.forName("org.h2.server.web.JakartaWebServlet");
            Servlet console = (Servlet) consoleType.getDeclaredConstructor().newInstance();
            return new ServletRegistrationBean<>(console, path, path + "/*");
        } catch (ReflectiveOperationException | ClassCastException exception) {
            throw new IllegalStateException("H2 console is enabled but its servlet could not be created", exception);
        }
    }
}
