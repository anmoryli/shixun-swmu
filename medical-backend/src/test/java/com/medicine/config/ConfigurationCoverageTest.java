package com.medicine.config;

import com.medicine.business.config.UploadWebConfig;
import com.medicine.business.service.FileStorageService;
import com.medicine.security.RestAccessDeniedHandler;
import com.medicine.security.RestAuthenticationEntryPoint;
import com.medicine.security.TokenAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigurationCoverageTest {
    @Test
    void securityBeansConfigurePasswordsCorsAndFilterChain() throws Exception {
        SecurityConfig config = new SecurityConfig(mock(TokenAuthenticationFilter.class),
                mock(RestAuthenticationEntryPoint.class), mock(RestAccessDeniedHandler.class));
        assertInstanceOf(BCryptPasswordEncoder.class, config.passwordEncoder());
        CorsConfigurationSource source = config.corsConfigurationSource(" https://one.test, ,https://two.test ");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        CorsConfiguration cors = source.getCorsConfiguration(request);
        assertNotNull(cors);
        assertEquals(2, cors.getAllowedOrigins().size());
        assertTrue(cors.getAllowCredentials());
        assertEquals(3600L, cors.getMaxAge());

        ObjectPostProcessor<Object> postProcessor = new ObjectPostProcessor<>() {
            @Override
            public <O> O postProcess(O object) {
                return object;
            }
        };
        AuthenticationManagerBuilder authenticationBuilder = new AuthenticationManagerBuilder(postProcessor);
        Map<Class<?>, Object> sharedObjects = new HashMap<>();
        sharedObjects.put(AuthenticationManagerBuilder.class, authenticationBuilder);
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        applicationContext.getBeanFactory().registerSingleton(
                "mvcHandlerMappingIntrospector", new HandlerMappingIntrospector());
        sharedObjects.put(ApplicationContext.class, applicationContext);
        HttpSecurity http = new HttpSecurity(postProcessor, authenticationBuilder, sharedObjects);
        http.authenticationManager(mock(AuthenticationManager.class));
        assertNotNull(config.securityFilterChain(http));
    }

    @Test
    void uploadResourcesNormalizeDirectoryUris() {
        FileStorageService storage = mock(FileStorageService.class);
        Path path = mock(Path.class);
        when(storage.directory()).thenReturn(path);
        ResourceHandlerRegistry registry = mock(ResourceHandlerRegistry.class, RETURNS_DEEP_STUBS);

        when(path.toUri()).thenReturn(URI.create("file:/tmp/uploads"));
        new UploadWebConfig(storage).addResourceHandlers(registry);
        verify(registry).addResourceHandler("/image/**");

        reset(registry);
        when(registry.addResourceHandler(anyString())).thenReturn(mock(org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration.class));
        when(path.toUri()).thenReturn(URI.create("file:/tmp/uploads/"));
        new UploadWebConfig(storage).addResourceHandlers(registry);
        verify(registry).addResourceHandler("/image/**");
    }
}
