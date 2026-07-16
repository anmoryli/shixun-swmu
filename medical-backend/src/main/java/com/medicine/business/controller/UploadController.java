/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.business.controller;

import com.medicine.business.service.FileStorageService;
import com.medicine.common.ApiResponse;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
    @PreAuthorize("hasAuthority('file:upload')")
    public ApiResponse<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        String fileName = storageService.saveImage(file);
        // 返回同源相对路径：绝对 URL 在 https 前端会触发混合内容拦截，
        // 且经反代后 host/protocol 不可靠。nginx /image/ 优先服务前端
        // public/image 内置样例图，未命中再回源后端 /app/uploads 上传目录。
        String url = "/image/" + fileName;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("url", url);
        return ApiResponse.success(data);
    }
}
