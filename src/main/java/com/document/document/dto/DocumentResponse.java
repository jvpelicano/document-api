package com.document.document.dto;

/*
 * @created 24/05/2025 - 2:56 PM
 * @project document
 * @author Janice Pelicano
 */
public class DocumentResponse {
    private Long id;
    private String message;
    private String status;

    public DocumentResponse(Long id, String message, String status) {
        this.id = id;
        this.message = message;
        this.status = status;
    }

    // Getters
    public Long getId() { return id; }
    public String getMessage() { return message; }
    public String getStatus() { return status; }
}

