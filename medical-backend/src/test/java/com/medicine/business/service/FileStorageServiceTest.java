/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.business.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.medicine.common.BusinessException;
import com.medicine.common.ErrorCode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

class FileStorageServiceTest {

    private static final byte[] ONE_PIXEL_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
    );

    @TempDir
    Path tempDirectory;

    @Test
    void savesValidatedPngWithGeneratedName() throws IOException {
        FileStorageService service = new FileStorageService(tempDirectory.toString());
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", ONE_PIXEL_PNG);

        String fileName = service.saveImage(file);

        assertThat(fileName).endsWith(".png").doesNotContain("avatar");
        assertThat(service.directory()).isEqualTo(tempDirectory.toAbsolutePath().normalize());
        assertThat(Files.readAllBytes(tempDirectory.resolve(fileName))).isEqualTo(ONE_PIXEL_PNG);
    }

    @Test
    void rejectsMissingAndEmptyFiles() {
        FileStorageService service = new FileStorageService(tempDirectory.toString());

        assertInvalid(() -> service.saveImage(null), "请选择");
        assertInvalid(() -> service.saveImage(
                new MockMultipartFile("file", "empty.png", "image/png", new byte[0])), "请选择");
    }

    @Test
    void rejectsUnsupportedContentTypes() {
        FileStorageService service = new FileStorageService(tempDirectory.toString());

        assertInvalid(() -> service.saveImage(
                new MockMultipartFile("file", "payload.svg", "image/svg+xml", "<svg/>".getBytes())), "仅支持");
    }

    @Test
    void rejectsFilesOverTwoMegabytesBeforeReadingThem() {
        FileStorageService service = new FileStorageService(tempDirectory.toString());
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getSize()).thenReturn(2L * 1024 * 1024 + 1);

        assertInvalid(() -> service.saveImage(file), "2MB");
    }

    @Test
    void rejectsSpoofedImageContent() {
        FileStorageService service = new FileStorageService(tempDirectory.toString());

        assertInvalid(() -> service.saveImage(
                new MockMultipartFile("file", "fake.png", "image/png", "not-an-image".getBytes())), "图片内容无效");
    }

    @Test
    void mapsUnreadableInputToAnInvalidArgument() throws IOException {
        FileStorageService service = new FileStorageService(tempDirectory.toString());
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getSize()).thenReturn(100L);
        when(file.getInputStream()).thenThrow(new IOException("unreadable"));

        assertInvalid(() -> service.saveImage(file), "无法读取");
    }

    @Test
    void mapsStorageFailuresToInternalError() throws IOException {
        Path occupiedPath = tempDirectory.resolve("occupied");
        Files.writeString(occupiedPath, "not a directory");
        FileStorageService service = new FileStorageService(occupiedPath.toString());
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", ONE_PIXEL_PNG);

        assertThatThrownBy(() -> service.saveImage(file))
                .isInstanceOf(BusinessException.class)
                .extracting("code", "message")
                .containsExactly(ErrorCode.INTERNAL_ERROR, "图片保存失败");
    }

    @Test
    void rejectsTraversalWhenResolvingGeneratedTargets() {
        FileStorageService service = new FileStorageService(tempDirectory.toString());
        assertThatThrownBy(() -> service.resolveTarget("../outside.png"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.INVALID_ARGUMENT);
    }

    private void assertInvalid(Runnable operation, String messageFragment) {
        assertThatThrownBy(operation::run)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(messageFragment)
                .extracting("code")
                .isEqualTo(ErrorCode.INVALID_ARGUMENT);
    }
}
