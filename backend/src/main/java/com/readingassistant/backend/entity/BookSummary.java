package com.readingassistant.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "book_summaries")
public class BookSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "book_page_id",
            nullable = false,
            unique = true
    )
    private BookPage bookPage;

    @Column(
            columnDefinition = "TEXT",
            nullable = false
    )
    private String content;

    private String model;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public BookSummary() {
    }

    public BookSummary(
            BookPage bookPage,
            String content,
            String model
    ) {
        this.bookPage = bookPage;
        this.content = content;
        this.model = model;
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now =
                LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt =
                LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public BookPage getBookPage() {
        return bookPage;
    }

    public void setBookPage(
            BookPage bookPage
    ) {
        this.bookPage = bookPage;
    }

    public String getContent() {
        return content;
    }

    public void setContent(
            String content
    ) {
        this.content = content;
    }

    public String getModel() {
        return model;
    }

    public void setModel(
            String model
    ) {
        this.model = model;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}