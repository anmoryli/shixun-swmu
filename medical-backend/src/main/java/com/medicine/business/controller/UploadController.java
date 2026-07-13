package com.medicine.business.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.medicine.business.service.FileStorageService;
import com.medicine.common.ApiResponse;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/base")
public class UploadController {
    private final FileStorageService storageService;

    public UploadController(FileStorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/upload")
    @PreAuthorize("hasRole('1')")
    public ApiResponse<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        String fileName = storageService.saveImage(file);
        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/image/").path(fileName).toUriString();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("url", url);
        return ApiResponse.success(data);
    }
}
