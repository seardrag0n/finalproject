package com.readingassistant.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "book_questions")
public class BookQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 하나의 BookPage에 여러 질문이 존재할 수 있음
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "book_page_id",
            nullable = false
    )
    private BookPage bookPage;

    @Column(
            columnDefinition = "TEXT",
            nullable = false
    )
    private String question;

    @Column(
            columnDefinition = "TEXT",
            nullable = false
    )
    private String answer;

    private String model;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // JPA 기본 생성자
    protected BookQuestion() {
    }

    public BookQuestion(
            BookPage bookPage,
            String question,
            String answer,
            String model
    ) {
        this.bookPage = bookPage;
        this.question = question;
        this.answer = answer;
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

    public String getQuestion() {
        return question;
    }

    public void setQuestion(
            String question
    ) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(
            String answer
    ) {
        this.answer = answer;
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