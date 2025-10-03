package io.config;

import jakarta.servlet.*;

import java.util.EnumSet;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.DispatcherServlet;

@Slf4j
public class WebAppInitializer implements WebApplicationInitializer {
    @Override
    public void onStartup(ServletContext servletContext) {

        AnnotationConfigWebApplicationContext root = new AnnotationConfigWebApplicationContext();
        root.register(AppConfig.class, DataBaseConfig.class);
        servletContext.addListener(new ContextLoaderListener(root));

        AnnotationConfigWebApplicationContext web = new AnnotationConfigWebApplicationContext();
        web.register(WebConfig.class);

        ServletRegistration.Dynamic dispatcher =
                servletContext.addServlet("dispatcher", new DispatcherServlet(web));
        dispatcher.setLoadOnStartup(1);
        dispatcher.addMapping("/");

        FilterRegistration.Dynamic fr = servletContext.addFilter(
                "sessionAuthFilter", DelegatingFilterProxy.class);
        fr.addMappingForUrlPatterns(
                EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.ASYNC),
                false, "/*");

        log.info("✅ Spring contexts and filter initialized.");
    }
}
