package com.readingassistant.backend.service;

import com.readingassistant.backend.entity.Book;
import com.readingassistant.backend.entity.BookMemo;
import com.readingassistant.backend.entity.BookPage;
import com.readingassistant.backend.entity.BookSummary;
import com.readingassistant.backend.entity.BookQuestion;

import com.readingassistant.backend.repository.BookMemoRepository;
import com.readingassistant.backend.repository.BookPageRepository;
import com.readingassistant.backend.repository.BookRepository;
import com.readingassistant.backend.repository.BookSummaryRepository;
import com.readingassistant.backend.repository.BookQuestionRepository;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final BookPageRepository bookPageRepository;
    private final BookMemoRepository bookMemoRepository;
    private final BookSummaryRepository bookSummaryRepository;
    private final BookQuestionRepository bookQuestionRepository;

    private final Path uploadDirectory =
            Paths.get("uploads");

    // =========================
    // Constructor
    // =========================

    public BookService(
            BookRepository bookRepository,
            BookPageRepository bookPageRepository,
            BookMemoRepository bookMemoRepository,
            BookSummaryRepository bookSummaryRepository,
            BookQuestionRepository bookQuestionRepository
    ) {
        this.bookRepository = bookRepository;
        this.bookPageRepository = bookPageRepository;
        this.bookMemoRepository = bookMemoRepository;
        this.bookSummaryRepository = bookSummaryRepository;
        this.bookQuestionRepository = bookQuestionRepository;
    }

    // =========================
    // Book
    // =========================

    public Book saveBook(Book book) {
        return bookRepository.save(book);
    }

    public List<Book> getBooks() {
        return bookRepository.findAll();
    }

    public Book getBook(Long bookId) {

        return bookRepository
                .findById(bookId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Book not found"
                        )
                );
    }

    // =========================
    // Last Read Page
    // =========================

    public Book updateLastReadPage(
            Long bookId,
            Integer pageNumber
    ) {

        Book book =
                bookRepository
                        .findById(bookId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Book not found"
                                )
                        );

        if (
                pageNumber == null ||
                pageNumber < 1
        ) {
            throw new IllegalArgumentException(
                    "Page number must be greater than 0"
            );
        }

        if (
                book.getTotalPages() != null &&
                pageNumber > book.getTotalPages()
        ) {
            throw new IllegalArgumentException(
                    "Page number exceeds total pages"
            );
        }

        book.setLastReadPage(pageNumber);

        return bookRepository.save(book);
    }

    // =========================
    // PDF Upload
    // =========================

    public Book uploadBook(
            String title,
            String author,
            String genre,
            MultipartFile file
    ) throws IOException {

        Files.createDirectories(
                uploadDirectory
        );

        String originalFilename =
                file.getOriginalFilename();

        String storedFilename =
                UUID.randomUUID() + ".pdf";

        Path filePath =
                uploadDirectory.resolve(
                        storedFilename
                );

        Files.copy(
                file.getInputStream(),
                filePath,
                StandardCopyOption.REPLACE_EXISTING
        );

        try (
                PDDocument document =
                        Loader.loadPDF(
                                filePath.toFile()
                        )
        ) {

            int totalPages =
                    document.getNumberOfPages();

            // =========================
            // Book 저장
            // =========================

            Book book = new Book();

            book.setTitle(title);
            book.setAuthor(author);
            book.setGenre(genre);

            book.setOriginalFilename(
                    originalFilename
            );

            book.setFilePath(
                    filePath.toString()
            );

            book.setTotalPages(
                    totalPages
            );

            Book savedBook =
                    bookRepository.save(book);

            // =========================
            // 페이지별 텍스트 추출
            // =========================

            PDFTextStripper stripper =
                    new PDFTextStripper();

            for (
                    int pageNumber = 1;
                    pageNumber <= totalPages;
                    pageNumber++
            ) {

                stripper.setStartPage(
                        pageNumber
                );

                stripper.setEndPage(
                        pageNumber
                );

                String content =
                        stripper.getText(
                                document
                        );

                BookPage bookPage =
                        new BookPage(
                                savedBook,
                                pageNumber,
                                content
                        );

                bookPageRepository.save(
                        bookPage
                );
            }

            return savedBook;
        }
    }

    // =========================
    // Book Page
    // =========================

    public BookPage getBookPage(
            Long bookId,
            Integer pageNumber
    ) {

        return bookPageRepository
                .findByBookIdAndPageNumber(
                        bookId,
                        pageNumber
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Page not found"
                        )
                );
    }

    // =========================
    // Memo
    // =========================

    public BookMemo getMemo(
            Long bookId,
            Integer pageNumber
    ) {

        BookPage bookPage =
                getBookPage(
                        bookId,
                        pageNumber
                );

        return bookMemoRepository
                .findByBookPageId(
                        bookPage.getId()
                )
                .orElse(null);
    }

    public BookMemo saveMemo(
            Long bookId,
            Integer pageNumber,
            String content
    ) {

        BookPage bookPage =
                getBookPage(
                        bookId,
                        pageNumber
                );

        BookMemo memo =
                bookMemoRepository
                        .findByBookPageId(
                                bookPage.getId()
                        )
                        .orElseGet(() ->
                                new BookMemo(
                                        bookPage,
                                        content
                                )
                        );

        memo.setContent(content);

        return bookMemoRepository.save(
                memo
        );
    }

    // =========================
    // Summary
    // =========================

    public BookSummary getSummary(
            Long bookId,
            Integer pageNumber
    ) {

        BookPage bookPage =
                getBookPage(
                        bookId,
                        pageNumber
                );

        return bookSummaryRepository
                .findByBookPageId(
                        bookPage.getId()
                )
                .orElse(null);
    }

    public BookSummary saveSummary(
            Long bookId,
            Integer pageNumber,
            String content,
            String model
    ) {

        BookPage bookPage =
                getBookPage(
                        bookId,
                        pageNumber
                );

        BookSummary summary =
                bookSummaryRepository
                        .findByBookPageId(
                                bookPage.getId()
                        )
                        .orElseGet(() ->
                                new BookSummary(
                                        bookPage,
                                        content,
                                        model
                                )
                        );

        summary.setContent(content);
        summary.setModel(model);

        return bookSummaryRepository.save(
                summary
        );
    }

    // =========================
    // Question
    // =========================

    /*
     * 현재 페이지에 저장된
     * 모든 질문/답변 조회
     *
     * 오래된 질문부터 순서대로 반환
     */
    public List<BookQuestion> getQuestions(
            Long bookId,
            Integer pageNumber
    ) {

        BookPage bookPage =
                getBookPage(
                        bookId,
                        pageNumber
                );

        return bookQuestionRepository
                .findByBookPageIdOrderByCreatedAtAsc(
                        bookPage.getId()
                );
    }

    /*
     * Gemini가 생성한 질문/답변을
     * DB에 저장
     */
    public BookQuestion saveQuestion(
            Long bookId,
            Integer pageNumber,
            String question,
            String answer,
            String model
    ) {

        if (
                question == null ||
                question.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Question cannot be empty"
            );
        }

        if (
                answer == null ||
                answer.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Answer cannot be empty"
            );
        }

        BookPage bookPage =
                getBookPage(
                        bookId,
                        pageNumber
                );

        BookQuestion bookQuestion =
                new BookQuestion(
                        bookPage,
                        question,
                        answer,
                        model
                );

        return bookQuestionRepository.save(
                bookQuestion
        );
    }
}