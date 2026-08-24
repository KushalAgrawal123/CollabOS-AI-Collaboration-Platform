package com.collabos.backend.storage;

import com.collabos.backend.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Local disk storage, deliberately — this is a free, local-only project, and
 * a real object store (S3 etc.) would be the first thing to cost money or
 * need an account. Files live under app.storage.root-dir, named by a random
 * UUID rather than anything derived from client input, which is what rules
 * out path traversal (../../etc) and cross-upload filename collisions in one
 * move — the original name is kept only as DB metadata for display/download.
 */
@Service
public class FileStorageService {

    private final Path rootDir;

    public FileStorageService(@Value("${app.storage.root-dir}") String rootDir) {
        this.rootDir = Path.of(rootDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create upload storage directory: " + this.rootDir, e);
        }
    }

    public String store(MultipartFile file, String extension) {
        String storedFileName = UUID.randomUUID() + extension;
        Path target = rootDir.resolve(storedFileName);
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store the uploaded file");
        }
        return storedFileName;
    }

    public Resource load(String storedFileName) {
        Path file = rootDir.resolve(storedFileName).normalize();
        if (!file.startsWith(rootDir)) {
            // storedFileName is always our own UUID, so this should be unreachable —
            // it's here as a defense-in-depth guard, not a path we expect to hit.
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid file reference");
        }
        try {
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ApiException(HttpStatus.NOT_FOUND, "File not found on disk");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new ApiException(HttpStatus.NOT_FOUND, "File not found on disk");
        }
    }

    public void delete(String storedFileName) {
        try {
            Files.deleteIfExists(rootDir.resolve(storedFileName).normalize());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete stored file: " + storedFileName, e);
        }
    }
}
