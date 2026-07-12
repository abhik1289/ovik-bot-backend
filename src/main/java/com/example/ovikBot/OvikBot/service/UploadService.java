package com.example.ovikBot.OvikBot.service;


import com.example.ovikBot.OvikBot.exception.InvalidUploadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;

@Service
@Slf4j
@RequiredArgsConstructor
public class UploadService {

    private final ChunkService chunkService;
    private  final VectorStoreService vectorStoreService;
    private static final String UPLOAD_DIR = "uploads";

    public void upload(MultipartFile file) {
        try {
            validateFile(file);

            Files.createDirectories(Paths.get(UPLOAD_DIR));

            Path path = Paths.get(
                    UPLOAD_DIR,
                    Path.of(file.getOriginalFilename()).getFileName().toString()
            );

            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

            log.info("File uploaded successfully.");
            log.info("PATH IS:::---> {}", path);

            PagePdfDocumentReader reader =
                    new PagePdfDocumentReader(
                            new FileSystemResource(path)
                    );

            List<Document> documents = reader.get();
            log.info("Pages : {}", documents.size());

            if (documents.isEmpty()) {
                throw new InvalidUploadException("The PDF was read, but no text could be extracted. The file may be scanned, image-only, or corrupted.");
            }

            List<Document> chunks = chunkService.chunk(documents);
            vectorStoreService.save(chunks);
            log.info("Chunks : {}", chunks.size());

        } catch (IOException e) {
            log.error("Failed to process uploaded PDF", e);
            throw new InvalidUploadException("Invalid or unreadable PDF file.", e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidUploadException("Please upload a non-empty PDF file.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new InvalidUploadException("Only PDF uploads are supported.");
        }
    }

}
