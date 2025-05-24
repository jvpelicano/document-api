package com.document.document.service;

import com.document.document.entity.Document;
import com.document.document.entity.ProcessingStatus;
import com.document.document.repository.DocumentRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/*
 * @created 24/05/2025 - 3:07 PM
 * @project document
 * @author Janice Pelicano
 */
@Service
public class DocumentProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingService.class);
    private static final int CHUNK_SIZE = 8192; // 8KB chunks
    private static final String UPLOAD_DIR = "uploads/";

    @Autowired
    private DocumentRepository documentRepository;

    public Document initiateDocumentSave(MultipartFile file) throws IOException {
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String uniqueFilename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        String filePath = UPLOAD_DIR + uniqueFilename;

        // Create document record
        Document document = new Document(
                file.getOriginalFilename(),
                filePath,
                file.getSize()
        );

        document = documentRepository.save(document);

        // Start asynchronous processing
        processDocumentAsync(document.getId(), file);

        return document;
    }

    @Async("documentProcessorExecutor")
    @Transactional
    public void processDocumentAsync(Long documentId, MultipartFile file) {
        Document document = documentRepository.findById(documentId).orElse(null);
        if (document == null) {
            logger.error("Document with ID {} not found", documentId);
            CompletableFuture.completedFuture(null);
            return;
        }

        try {
            updateDocumentStatus(documentId, ProcessingStatus.IN_PROGRESS, 0);

            // Stream the file in chunks to avoid memory issues
            saveFileInChunks(document, file);

            // Simulate additional processing (validation, virus scan, etc.)
            performPostProcessing(document);

            updateDocumentStatus(documentId, ProcessingStatus.COMPLETED, 100);
            updateCompletionTime(documentId);

            logger.info("Successfully processed document: {}", document.getFilename());

        } catch (Exception e) {
            logger.error("Error processing document {}: {}", document.getFilename(), e.getMessage(), e);
            updateDocumentWithError(documentId, e.getMessage());
        }

        CompletableFuture.completedFuture(null);
    }

    private void saveFileInChunks(Document document, MultipartFile file) throws IOException {
        Path filePath = Paths.get(document.getFilePath());

        try (InputStream inputStream = file.getInputStream();
             BufferedInputStream bufferedInput = new BufferedInputStream(inputStream);
             FileOutputStream fileOutput = new FileOutputStream(filePath.toFile());
             BufferedOutputStream bufferedOutput = new BufferedOutputStream(fileOutput)) {

            byte[] buffer = new byte[CHUNK_SIZE];
            long totalBytesRead = 0;
            int bytesRead;

            while ((bytesRead = bufferedInput.read(buffer)) != -1) {
                bufferedOutput.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;

                // Update progress
                int progress = (int) ((totalBytesRead * 100) / document.getFileSize());
                updateProgress(document.getId(), progress);

                // Simulate processing time and allow for interruption
                Thread.sleep(10); // Small delay to simulate processing

                // Check if processing should be cancelled
                Document currentDoc = documentRepository.findById(document.getId()).orElse(null);
                if (currentDoc != null && currentDoc.getStatus() == ProcessingStatus.CANCELLED) {
                    throw new InterruptedException("Processing cancelled by user");
                }
            }

            bufferedOutput.flush();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Document processing was cancelled", e);
        }
    }

    private void performPostProcessing(Document document) throws InterruptedException {
        // Simulate additional processing steps
        for (int i = 0; i < 5; i++) {
            Thread.sleep(200); // Simulate processing time

            // Check for cancellation
            Document currentDoc = documentRepository.findById(document.getId()).orElse(null);
            if (currentDoc != null && currentDoc.getStatus() == ProcessingStatus.CANCELLED) {
                throw new InterruptedException("Processing cancelled during post-processing");
            }
        }
    }

    @Transactional
    public void updateDocumentStatus(Long documentId, ProcessingStatus status, Integer progress) {
        documentRepository.findById(documentId).ifPresent(doc -> {
            doc.setStatus(status);
            if (progress != null) {
                doc.setProgress(progress);
            }
            documentRepository.save(doc);
        });
    }

    @Transactional
    public void updateProgress(Long documentId, Integer progress) {
        documentRepository.findById(documentId).ifPresent(doc -> {
            doc.setProgress(progress);
            documentRepository.save(doc);
        });
    }

    @Transactional
    public void updateCompletionTime(Long documentId) {
        documentRepository.findById(documentId).ifPresent(doc -> {
            doc.setCompletedAt(LocalDateTime.now());
            documentRepository.save(doc);
        });
    }

    @Transactional
    public void updateDocumentWithError(Long documentId, String errorMessage) {
        documentRepository.findById(documentId).ifPresent(doc -> {
            doc.setStatus(ProcessingStatus.FAILED);
            doc.setErrorMessage(errorMessage);
            doc.setCompletedAt(LocalDateTime.now());
            documentRepository.save(doc);
        });
    }

    public Document getDocumentStatus(Long documentId) {
        return documentRepository.findById(documentId).orElse(null);
    }

    @Transactional
    public boolean cancelDocumentProcessing(Long documentId) {
        Document document = documentRepository.findById(documentId).orElse(null);
        if (document != null && document.getStatus() == ProcessingStatus.IN_PROGRESS) {
            document.setStatus(ProcessingStatus.CANCELLED);
            document.setCompletedAt(LocalDateTime.now());
            documentRepository.save(document);

            // Clean up partial file
            try {
                Files.deleteIfExists(Paths.get(document.getFilePath()));
            } catch (IOException e) {
                logger.warn("Could not delete partial file: {}", document.getFilePath());
            }

            return true;
        }
        return false;
    }
}
