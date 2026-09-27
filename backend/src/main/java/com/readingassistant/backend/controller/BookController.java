package com.readingassistant.backend.controller;

import com.readingassistant.backend.entity.Book;
import com.readingassistant.backend.entity.BookMemo;
import com.readingassistant.backend.entity.BookPage;
import com.readingassistant.backend.entity.BookSummary;
import com.readingassistant.backend.entity.BookQuestion;

import com.readingassistant.backend.service.BookService;
import com.readingassistant.backend.service.GeminiService;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/books")
@CrossOrigin(origins = "http://localhost:3000")
public class BookController {

    private final BookService bookService;
    private final GeminiService geminiService;

    // =========================
    // Constructor
    // =========================

    public BookController(
            BookService bookService,
            GeminiService geminiService
    ) {
        this.bookService = bookService;
        this.geminiService = geminiService;
    }

    // =========================
    // Book
    // =========================

    @PostMapping
    public Book createBook(
            @RequestBody Book book
    ) {

        return bookService.saveBook(
                book
        );
    }

    @GetMapping
    public List<Book> getBooks() {

        return bookService.getBooks();
    }

    @GetMapping("/{bookId}")
    public Book getBook(
            @PathVariable Long bookId
    ) {

        return bookService.getBook(
                bookId
        );
    }

    // =========================
    // Reading Progress
    // =========================

    @PatchMapping("/{bookId}/progress")
    public Book updateProgress(
            @PathVariable Long bookId,
            @RequestBody ProgressRequest request
    ) {

        return bookService.updateLastReadPage(
                bookId,
                request.pageNumber()
        );
    }

    public record ProgressRequest(
            Integer pageNumber
    ) {
    }

    // =========================
    // PDF Upload
    // =========================

    @PostMapping("/upload")
    public Book uploadBook(
            @RequestParam String title,

            @RequestParam(required = false)
            String author,

            @RequestParam(required = false)
            String genre,

            @RequestParam("file")
            MultipartFile file
    ) throws IOException {

        return bookService.uploadBook(
                title,
                author,
                genre,
                file
        );
    }

    // =========================
    // Book Page
    // =========================

    @GetMapping(
            "/{bookId}/pages/{pageNumber}"
    )
    public BookPage getBookPage(
            @PathVariable Long bookId,
            @PathVariable Integer pageNumber
    ) {

        return bookService.getBookPage(
                bookId,
                pageNumber
        );
    }

    // =========================
    // PDF
    // =========================

    @GetMapping("/{bookId}/pdf")
    public ResponseEntity<Resource> getBookPdf(
            @PathVariable Long bookId
    ) throws IOException {

        Book book =
                bookService.getBook(
                        bookId
                );

        Path path =
                Paths.get(
                        book.getFilePath()
                )
                .toAbsolutePath()
                .normalize();

        Resource resource =
                new UrlResource(
                        path.toUri()
                );

        if (!resource.exists()) {

            return ResponseEntity
                    .notFound()
                    .build();
        }

        return ResponseEntity
                .ok()
                .contentType(
                        MediaType.APPLICATION_PDF
                )
                .body(resource);
    }

    // =========================
    // Memo
    // =========================

    @GetMapping(
            "/{bookId}/pages/{pageNumber}/memo"
    )
    public BookMemo getMemo(
            @PathVariable Long bookId,
            @PathVariable Integer pageNumber
    ) {

        return bookService.getMemo(
                bookId,
                pageNumber
        );
    }

    @PutMapping(
            "/{bookId}/pages/{pageNumber}/memo"
    )
    public BookMemo saveMemo(
            @PathVariable Long bookId,
            @PathVariable Integer pageNumber,
            @RequestBody MemoRequest request
    ) {

        return bookService.saveMemo(
                bookId,
                pageNumber,
                request.content()
        );
    }

    public record MemoRequest(
            String content
    ) {
    }

    // =========================
    // Summary
    // =========================

    /*
     * DB에 저장된 페이지 요약 조회
     *
     * 요약이 존재하지 않으면 null 반환
     */
    @GetMapping(
            "/{bookId}/pages/{pageNumber}/summary"
    )
    public BookSummary getSummary(
            @PathVariable Long bookId,
            @PathVariable Integer pageNumber
    ) {

        return bookService.getSummary(
                bookId,
                pageNumber
        );
    }

    /*
     * 페이지 요약 생성
     *
     * GeminiService 내부에서:
     *
     * 1. gemini-3.8-flash 1차 시도
     * 2. 503 발생 시 1초 대기
     * 3. gemini-3.8-flash 2차 시도
     * 4. 다시 503 발생 시 2초 대기
     * 5. gemini-3.6-flash로 fallback
     *
     * 실제 성공한 모델명과
     * 생성된 요약을 DB에 저장한다.
     */
    @PostMapping(
            "/{bookId}/pages/{pageNumber}/summary"
    )
    public BookSummary summarizePage(
            @PathVariable Long bookId,
            @PathVariable Integer pageNumber
    ) {

        // 1. 현재 페이지 조회
        BookPage bookPage =
                bookService.getBookPage(
                        bookId,
                        pageNumber
                );

        // 2. Gemini를 사용하여 요약 생성
        GeminiService.GeminiResult result =
                geminiService.summarizePage(
                        bookPage.getContent()
                );

        // 3. 요약 + 실제 사용 모델 DB 저장
        return bookService.saveSummary(
                bookId,
                pageNumber,
                result.content(),
                result.model()
        );
    }

    // =========================
    // Question
    // =========================

    /*
     * 해당 페이지에 저장되어 있는
     * 모든 질문/답변 조회
     */
    @GetMapping(
            "/{bookId}/pages/{pageNumber}/questions"
    )
    public List<BookQuestion> getQuestions(
            @PathVariable Long bookId,
            @PathVariable Integer pageNumber
    ) {

        return bookService.getQuestions(
                bookId,
                pageNumber
        );
    }

    /*
     * 현재 페이지에 대해 질문
     *
     * 1. 질문 내용 검증
     * 2. 현재 페이지 조회
     * 3. Gemini에 페이지 내용 + 질문 전달
     * 4. Gemini 답변 생성
     * 5. 질문 + 답변 + 실제 사용 모델 DB 저장
     */
    @PostMapping(
            "/{bookId}/pages/{pageNumber}/questions"
    )
    public BookQuestion askQuestion(
            @PathVariable Long bookId,
            @PathVariable Integer pageNumber,
            @RequestBody QuestionRequest request
    ) {

        // =========================
        // 1. 질문 검증
        // =========================

        if (
                request.question() == null ||
                request.question().isBlank()
        ) {

            throw new IllegalArgumentException(
                    "질문을 입력해주세요."
            );
        }

        // =========================
        // 2. 현재 페이지 조회
        // =========================

        BookPage bookPage =
                bookService.getBookPage(
                        bookId,
                        pageNumber
                );

        // =========================
        // 3. Gemini에게 질문
        // =========================

        GeminiService.GeminiResult result =
                geminiService.answerQuestion(
                        bookPage.getContent(),
                        request.question()
                );

        // =========================
        // 4. 질문 + 답변 DB 저장
        // =========================

        return bookService.saveQuestion(
                bookId,
                pageNumber,
                request.question(),
                result.content(),
                result.model()
        );
    }

    public record QuestionRequest(
            String question
    ) {
    }
}