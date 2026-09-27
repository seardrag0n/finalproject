package com.readingassistant.backend.repository;

import com.readingassistant.backend.entity.BookMemo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookMemoRepository
        extends JpaRepository<BookMemo, Long> {

    Optional<BookMemo> findByBookPageId(Long bookPageId);
}