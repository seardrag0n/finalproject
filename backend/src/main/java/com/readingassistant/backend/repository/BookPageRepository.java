package com.readingassistant.backend.repository;

import com.readingassistant.backend.entity.BookPage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookPageRepository extends JpaRepository<BookPage, Long> {

    Optional<BookPage> findByBookIdAndPageNumber(
            Long bookId,
            Integer pageNumber
    );
}