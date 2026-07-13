package com.medicine.business.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.medicine.business.service.FileStorageService;

@Configuration
public class UploadWebConfig implements WebMvcConfigurer {
    private final FileStorageService storageService;

    public UploadWebConfig(FileStorageService storageService) {
        this.storageService = storageService;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = storageService.directory().toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/image/**").addResourceLocations(location);
    }
}
