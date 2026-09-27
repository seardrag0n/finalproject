package com.readingassistant.backend.repository;

import com.readingassistant.backend.entity.BookSummary;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookSummaryRepository
        extends JpaRepository<BookSummary, Long> {

    Optional<BookSummary> findByBookPageId(
            Long bookPageId
    );
}