package com.aishop.commerce.storage;

import com.aishop.commerce.common.ApiResponse;
import com.aishop.commerce.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/files")
public class FileStorageController {
    private static final Set<String> TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private final Path root;

    public FileStorageController(@Value("${app.storage.path}") String path) { this.root = Path.of(path).toAbsolutePath(); }

    @PostMapping("/images")
    public ApiResponse<Map<String, String>> upload(@RequestPart("file") MultipartFile file) throws IOException {
        if (file.isEmpty() || file.getContentType() == null || !TYPES.contains(file.getContentType())) {
            throw new BusinessException("VALIDATION_ERROR", "仅支持 JPG、PNG、WebP 或 GIF 图片", HttpStatus.BAD_REQUEST);
        }
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String fileKey = UUID.randomUUID() + (extension == null ? "" : "." + extension.toLowerCase());
        Files.createDirectories(root);
        file.transferTo(root.resolve(fileKey));
        return ApiResponse.ok(Map.of("fileKey", fileKey, "url", "/uploads/" + fileKey));
    }
}
