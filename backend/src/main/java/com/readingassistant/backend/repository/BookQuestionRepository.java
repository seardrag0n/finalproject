package com.readingassistant.backend.repository;

import com.readingassistant.backend.entity.BookQuestion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookQuestionRepository
        extends JpaRepository<BookQuestion, Long> {

    List<BookQuestion> findByBookPageIdOrderByCreatedAtAsc(
            Long bookPageId
    );
}