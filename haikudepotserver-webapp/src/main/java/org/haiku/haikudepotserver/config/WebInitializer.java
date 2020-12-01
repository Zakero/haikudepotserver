/*
 * Copyright 2018-2020, Andrew Lindesay
 * Distributed under the terms of the MIT License.
 */

package org.haiku.haikudepotserver.config;

import com.codahale.metrics.servlets.HealthCheckServlet;
import com.codahale.metrics.servlets.MetricsServlet;
import com.codahale.metrics.servlets.PingServlet;
import net.jawr.web.servlet.JawrServlet;
import org.haiku.haikudepotserver.support.web.ErrorServlet;
import org.haiku.haikudepotserver.support.web.RemoteLogCaptureServlet;
import org.haiku.haikudepotserver.support.web.SessionListener;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;

/**
 * <p>This is a spring-construct that is auto-discovered in order to bootstrap the
 * servlet environment.  This (mostly) replaces the <code>web.xml</code> file.</p>
 */

public class WebInitializer implements WebApplicationInitializer {

    @Override
    public void onStartup(ServletContext servletContext) {

        AnnotationConfigWebApplicationContext rootContext = new AnnotationConfigWebApplicationContext();
        rootContext.register(AppConfig.class);

        servletContext.addListener(new ContextLoaderListener(rootContext));
        servletContext.addListener(new SessionListener());

        registerErrorServlet(servletContext);
        registerRemoteLogCaptureServlet(servletContext);
        registerSpringDispatcherServlet(servletContext);
        registerMetricsServlets(servletContext);
        registerJawrServlet(servletContext, "css");
        registerJawrServlet(servletContext, "js");

        // note that the spring security filters are not included here.

        registerSpringFilter(servletContext, "forwardedHeaderFilter", "/*");
        registerSpringFilter(servletContext, "metricsFilter", "/*");
        registerSpringFilter(servletContext, "springSecurityFilterChain", "/*");
        registerSpringFilter(servletContext, "loggingFilter", "/*");
        registerSpringFilter(servletContext, "singlePageTemplateFrequencyMetricsFilter", "/__js/app/*");
        registerSpringFilter(servletContext, "desktopApplicationMinimumVersionFilter", "/*");

        // would be nice to add the error handler here, but this not possible in this
        // mechanism right now evidently.

    }

    private void registerSpringFilter(
            ServletContext servletContext,
            String beanName,
            String... urlPatterns) {
        FilterRegistration.Dynamic dynamic = servletContext.addFilter(beanName, new DelegatingFilterProxy(beanName));
        dynamic.addMappingForUrlPatterns(null, false, urlPatterns);
        dynamic.setAsyncSupported(true);
    }

    private void registerJawrServlet(ServletContext servletContext, String type) {
        ServletRegistration.Dynamic dispatcher = servletContext.addServlet("jawr-servlet-" + type, JawrServlet.class);
        dispatcher.setInitParameter(
                "configPropertiesSourceClass",
                "org.haiku.haikudepotserver.support.web.SpringProvidedConfigPropertiesSource");
        dispatcher.setInitParameter("mapping", "/__jawr/" + type + "/");
        dispatcher.setInitParameter("type", type);
        dispatcher.setLoadOnStartup(1);
        dispatcher.addMapping("/__jawr/" + type + "/*");
        dispatcher.setAsyncSupported(true);
    }

    private void registerMetricsServlets(ServletContext servletContext) {
        ServletRegistration.Dynamic healthCheckDispatcher = servletContext.addServlet(
                "metric-admin-health-check", HealthCheckServlet.class);
        healthCheckDispatcher.setLoadOnStartup(1);
        healthCheckDispatcher.addMapping("/__metric/healthcheck");
        healthCheckDispatcher.setAsyncSupported(true);

        ServletRegistration.Dynamic adminDispatcher = servletContext.addServlet(
                "metric-admin-metrics",
                new MetricsServlet());
        adminDispatcher.setLoadOnStartup(1);
        adminDispatcher.addMapping("/__metric/metrics");
        adminDispatcher.setAsyncSupported(true);

        ServletRegistration.Dynamic pingDispatcher = servletContext.addServlet(
                "metric-admin-ping",
                new PingServlet());
        pingDispatcher.setLoadOnStartup(1);
        pingDispatcher.addMapping("/__metric/ping");
        pingDispatcher.setAsyncSupported(true);
    }

    private void registerErrorServlet(ServletContext servletContext) {
        ServletRegistration.Dynamic dispatcher = servletContext.addServlet("error-servlet", ErrorServlet.class);
        dispatcher.setLoadOnStartup(1);
        dispatcher.addMapping("/__error");
        dispatcher.setAsyncSupported(true);
    }

    private void registerRemoteLogCaptureServlet(ServletContext servletContext) {
        ServletRegistration.Dynamic dispatcher = servletContext.addServlet(
                "remote-log-capture", RemoteLogCaptureServlet.class);
        dispatcher.setLoadOnStartup(1);
        dispatcher.addMapping("/__log/capture");
        dispatcher.setAsyncSupported(true);
    }

    private void registerSpringDispatcherServlet(ServletContext servletContext) {
        AnnotationConfigWebApplicationContext dispatcherContext = new AnnotationConfigWebApplicationContext();
        dispatcherContext.register(ServletConfig.class);

        ServletRegistration.Dynamic dispatcher = servletContext.addServlet(
                "dispatcher",
                new DispatcherServlet(dispatcherContext));
        dispatcher.setLoadOnStartup(1);
        dispatcher.addMapping("/");
        dispatcher.setAsyncSupported(true);
    }

}
