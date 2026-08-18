package io.github.evertonsoethe.etlimporter.service;

import io.github.evertonsoethe.etlimporter.config.ImportProperties;
import io.github.evertonsoethe.etlimporter.exception.FileStorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final ImportProperties importProperties;

    public Path store(MultipartFile file) {
        try {
            Path uploadDir = Path.of(importProperties.getUploadDir());
            Files.createDirectories(uploadDir);

            String filename = UUID.randomUUID() + ".csv";
            Path target = uploadDir.resolve(filename);

            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            log.info("File stored: {} ({} bytes)", target.toAbsolutePath(), file.getSize());
            return target.toAbsolutePath();

        } catch (IOException e) {
            throw new FileStorageException("Failed to store uploaded file", e);
        }
    }
}
