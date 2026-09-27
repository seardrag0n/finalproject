package com.readingassistant.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key}")
    private String apiKey;

    // =========================
    // Model
    // =========================

    private static final String PRIMARY_MODEL =
            "gemini-3.8-flash";

    private static final String FALLBACK_MODEL =
            "gemini-3.6-flash";

    // =========================
    // Constructor
    // =========================

    public GeminiService() {
        this.restTemplate =
                new RestTemplate();

        this.objectMapper =
                new ObjectMapper();
    }

    // =========================
    // Gemini Result
    // =========================

    /*
     * Gemini가 반환한 실제 내용과
     * 실제 성공한 모델명을 함께 저장한다.
     */
    public record GeminiResult(
            String content,
            String model
    ) {
    }

    // =========================
    // Summary
    // =========================

    public GeminiResult summarizePage(
            String pageContent
    ) {

        if (
                pageContent == null ||
                pageContent.isBlank()
        ) {
            return new GeminiResult(
                    "이 페이지에서 추출된 텍스트가 없습니다.",
                    "none"
            );
        }

        String prompt = """
                다음은 사용자가 읽고 있는 책의 한 페이지입니다.

                이 페이지의 내용을 한국어로 이해하기 쉽게 요약하세요.

                규칙:
                - 핵심 내용을 중심으로 요약합니다.
                - 중요한 전문 용어는 원문 표현을 유지할 수 있습니다.
                - 원문에 없는 내용을 임의로 추가하지 않습니다.
                - 3~5개의 핵심 포인트로 정리합니다.
                - 마지막에 한 문장으로 핵심을 정리합니다.

                페이지 내용:

                """ + pageContent;

        return generateWithFallback(
                prompt
        );
    }

    // =========================
    // Question
    // =========================

    public GeminiResult answerQuestion(
            String pageContent,
            String question
    ) {

        if (
                pageContent == null ||
                pageContent.isBlank()
        ) {
            return new GeminiResult(
                    "이 페이지에서 추출된 텍스트가 없습니다.",
                    "none"
            );
        }

        if (
                question == null ||
                question.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "질문을 입력해주세요."
            );
        }

        String prompt = """
                다음은 사용자가 현재 읽고 있는 책의 한 페이지입니다.

                페이지 내용을 바탕으로 사용자의 질문에 답변하세요.

                규칙:
                - 페이지의 내용을 우선적으로 근거로 답변합니다.
                - 페이지에 직접 나오지 않은 내용이 필요한 경우,
                  일반적인 보충 설명임을 명확하게 구분합니다.
                - 원문에 없는 내용을 페이지에 있는 내용처럼 말하지 않습니다.
                - 중요한 전문 용어는 원문 표현을 함께 사용할 수 있습니다.
                - 사용자가 이해하기 쉽게 한국어로 설명합니다.
                - 질문과 직접 관련된 내용을 중심으로 답변합니다.

                [페이지 내용]

                """ + pageContent + """

                [사용자 질문]

                """ + question;

        return generateWithFallback(
                prompt
        );
    }

    // =========================
    // Fallback Logic
    // =========================

    private GeminiResult generateWithFallback(
            String prompt
    ) {

        // =========================
        // 1차 시도
        // gemini-3.8-flash
        // =========================

        try {

            return callGemini(
                    PRIMARY_MODEL,
                    prompt
            );

        } catch (
                HttpServerErrorException.ServiceUnavailable e
        ) {

            System.out.println(
                    "[Gemini] "
                            + PRIMARY_MODEL
                            + " 1차 호출 실패 (503)"
            );
        }

        // =========================
        // 1초 대기
        // =========================

        sleep(1000);

        // =========================
        // 2차 시도
        // gemini-3.8-flash
        // =========================

        try {

            return callGemini(
                    PRIMARY_MODEL,
                    prompt
            );

        } catch (
                HttpServerErrorException.ServiceUnavailable e
        ) {

            System.out.println(
                    "[Gemini] "
                            + PRIMARY_MODEL
                            + " 2차 호출 실패 (503)"
            );
        }

        // =========================
        // 2초 대기
        // =========================

        sleep(2000);

        // =========================
        // 3차 시도
        // gemini-3.6-flash
        // =========================

        System.out.println(
                "[Gemini] fallback 모델 사용: "
                        + FALLBACK_MODEL
        );

        return callGemini(
                FALLBACK_MODEL,
                prompt
        );
    }

    // =========================
    // Gemini API Call
    // =========================

    private GeminiResult callGemini(
            String model,
            String prompt
    ) {

        String url =
                "https://generativelanguage.googleapis.com/v1beta/models/"
                        + model
                        + ":generateContent";

        // =========================
        // Header
        // =========================

        HttpHeaders headers =
                new HttpHeaders();

        headers.setContentType(
                MediaType.APPLICATION_JSON
        );

        headers.set(
                "x-goog-api-key",
                apiKey
        );

        // =========================
        // Request Body
        // =========================

        Map<String, Object> body =
                Map.of(
                        "contents",
                        List.of(
                                Map.of(
                                        "parts",
                                        List.of(
                                                Map.of(
                                                        "text",
                                                        prompt
                                                )
                                        )
                                )
                        )
                );

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(
                        body,
                        headers
                );

        // =========================
        // API Request
        // =========================

        System.out.println(
                "[Gemini] API 호출: "
                        + model
        );

        ResponseEntity<String> response =
                restTemplate.postForEntity(
                        url,
                        request,
                        String.class
                );

        // =========================
        // Response Check
        // =========================

        HttpStatusCode statusCode =
                response.getStatusCode();

        if (!statusCode.is2xxSuccessful()) {

            throw new RuntimeException(
                    "Gemini API 호출 실패: "
                            + statusCode
            );
        }

        // =========================
        // JSON Parsing
        // =========================

        try {

            JsonNode root =
                    objectMapper.readTree(
                            response.getBody()
                    );

            String content =
                    root
                            .path("candidates")
                            .path(0)
                            .path("content")
                            .path("parts")
                            .path(0)
                            .path("text")
                            .asText();

            if (
                    content == null ||
                    content.isBlank()
            ) {

                throw new RuntimeException(
                        "Gemini 응답에 내용이 없습니다."
                );
            }

            System.out.println(
                    "[Gemini] 성공: "
                            + model
            );

            return new GeminiResult(
                    content,
                    model
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Gemini 응답 처리 실패",
                    e
            );
        }
    }

    // =========================
    // Sleep
    // =========================

    private void sleep(
            long milliseconds
    ) {

        try {

            Thread.sleep(
                    milliseconds
            );

        } catch (
                InterruptedException e
        ) {

            Thread.currentThread()
                    .interrupt();

            throw new RuntimeException(
                    "Gemini 재시도 대기 중 인터럽트 발생",
                    e
            );
        }
    }
}