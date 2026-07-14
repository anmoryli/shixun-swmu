/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.business.service;

import com.medicine.common.BusinessException;
import com.medicine.common.ErrorCode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;

@Service
public class FileStorageService {
    private static final long MAX_IMAGE_SIZE = 2L * 1024 * 1024;
    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png");

    private final Path directory;

    public FileStorageService(@Value("${app.upload.directory:uploads}") String directory) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    public String saveImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "请选择要上传的图片");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!IMAGE_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "仅支持 JPG 或 PNG 图片");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "图片大小不能超过 2MB");
        }
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null || image.getWidth() < 1 || image.getHeight() < 1
                    || image.getWidth() > 8000 || image.getHeight() > 8000) {
                throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "图片内容无效或尺寸过大");
            }
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "无法读取图片内容");
        }
        String extension = "image/png".equals(contentType) ? ".png" : ".jpg";
        String fileName = UUID.randomUUID() + extension;
        try {
            Files.createDirectories(directory);
            Path target = directory.resolve(fileName).normalize();
            if (!target.startsWith(directory)) {
                throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "非法文件名");
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "图片保存失败");
        }
    }

    public Path directory() {
        return directory;
    }
}
