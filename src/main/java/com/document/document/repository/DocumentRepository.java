package com.document.document.repository;

import com.document.document.entity.Document;
import com.document.document.entity.ProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/*
 * @created 24/05/2025 - 3:05 PM
 * @project document
 * @author Janice Pelicano
 */public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByStatus(ProcessingStatus status);

    @Query("SELECT d FROM Document d WHERE d.status = 'IN_PROGRESS' AND d.createdAt < :threshold")
    List<Document> findStaleProcessingDocuments(java.time.LocalDateTime threshold);
}


