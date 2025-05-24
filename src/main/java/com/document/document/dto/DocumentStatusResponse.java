package com.document.document.dto;

/*
 * @created 24/05/2025 - 2:57 PM
 * @project document
 * @author Janice Pelicano
 */
public class DocumentStatusResponse {
    private Long id;
    private String filename;
    private String status;
    private Integer progress;
    private String errorMessage;

    public DocumentStatusResponse(Long id, String filename, String status,
                                  Integer progress, String errorMessage) {
        this.id = id;
        this.filename = filename;
        this.status = status;
        this.progress = progress;
        this.errorMessage = errorMessage;
    }

    // Getters
    public Long getId() { return id; }
    public String getFilename() { return filename; }
    public String getStatus() { return status; }
    public Integer getProgress() { return progress; }
    public String getErrorMessage() { return errorMessage; }
}


