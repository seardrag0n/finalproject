package com.readingassistant.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "book_pages")
public class BookPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "page_number", nullable = false)
    private Integer pageNumber;

    @Lob
    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;

    public BookPage() {
    }

    public BookPage(Book book, Integer pageNumber, String content) {
        this.book = book;
        this.pageNumber = pageNumber;
        this.content = content;
    }

    public Long getId() {
        return id;
    }

    public Book getBook() {
        return book;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public String getContent() {
        return content;
    }
}