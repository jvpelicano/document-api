package com.document.document.controller;

import com.document.document.dto.DocumentResponse;
import com.document.document.dto.DocumentStatusResponse;
import com.document.document.entity.Document;
import com.document.document.service.DocumentProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/*
 * @created 24/05/2025 - 2:54 PM
 * @project document
 * @author Janice Pelicano
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);

    @Autowired
    private DocumentProcessingService documentProcessingService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadDocument(@RequestParam("file") MultipartFile file) {
        System.out.print("testto");
        logger.info("Received upload request for file: {}",
                file != null ? file.getOriginalFilename() : "null");

        try {
            // Validate file
            if (file == null) {
                logger.error("No file provided in request");
                return ResponseEntity.badRequest().body("No file provided");
            }

            if (file.isEmpty()) {
                logger.error("File is empty: {}", file.getOriginalFilename());
                return ResponseEntity.badRequest().body("File is empty");
            }

            if (file.getOriginalFilename() == null || file.getOriginalFilename().trim().isEmpty()) {
                logger.error("Invalid filename");
                return ResponseEntity.badRequest().body("Invalid filename");
            }

            logger.info("Processing file: {} (size: {} bytes)",
                    file.getOriginalFilename(), file.getSize());

            Document document = documentProcessingService.initiateDocumentSave(file);
            return ResponseEntity.ok(new DocumentResponse(
                    document.getId(),
                    "Document upload initiated successfully",
                    document.getStatus().toString()
            ));

        } catch (MultipartException e) {
            logger.error("Multipart parsing error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body("Invalid multipart request: " + e.getMessage());
        } catch (IOException e) {
            logger.error("IO error during file processing: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Failed to upload document: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during upload: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Unexpected error: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<?> getDocumentStatus(@PathVariable Long id) {
        Document document = documentProcessingService.getDocumentStatus(id);
        if (document == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(new DocumentStatusResponse(
                document.getId(),
                document.getFilename(),
                document.getStatus().toString(),
                document.getProgress(),
                document.getErrorMessage()
        ));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelDocumentProcessing(@PathVariable Long id) {
        boolean cancelled = documentProcessingService.cancelDocumentProcessing(id);
        if (cancelled) {
            return ResponseEntity.ok("Document processing cancelled successfully");
        } else {
            return ResponseEntity.badRequest().body("Cannot cancel document processing");
        }
    }
}