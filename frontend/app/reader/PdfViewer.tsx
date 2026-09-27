"use client";



import { useEffect, useMemo, useRef, useState } from "react";
import { useRouter } from "next/navigation";

import { Document, Page, pdfjs } from "react-pdf";

import styles from "./reader.module.css";

import ReactMarkdown from "react-markdown";



import "react-pdf/dist/Page/AnnotationLayer.css";

import "react-pdf/dist/Page/TextLayer.css";



pdfjs.GlobalWorkerOptions.workerSrc = new URL(

  "pdfjs-dist/build/pdf.worker.min.mjs",

  import.meta.url

).toString();



// =========================

// Config

// =========================



const API_URL = "http://localhost:8080/api/books";

type BookInfo = {
  id: number;
  title: string;
  author?: string | null;
  genre?: string | null;
  totalPages?: number | null;
  lastReadPage?: number | null;
};

type BookQuestion = {
  id: number;
  question: string;
  answer: string;
  model: string;
  createdAt: string;
  updatedAt: string;
};



export default function PdfViewer({ bookId }: { bookId: number }) {

  const router = useRouter();
  const pdfUrl = useMemo(() => `${API_URL}/${bookId}/pdf`, [bookId]);
  const [book, setBook] = useState<BookInfo | null>(null);
  const [bookLoading, setBookLoading] = useState(true);

  // =========================

  // PDF State

  // =========================



  const [numPages, setNumPages] = useState(0);

  const [pageNumber, setPageNumber] = useState(1);
  const [pdfWidth, setPdfWidth] = useState(900);
  const [pdfPageSize, setPdfPageSize] = useState({ width: 0, height: 0 });
  const pdfAreaRef = useRef<HTMLDivElement | null>(null);



  // =========================

  // Memo State

  // =========================



  const [memo, setMemo] = useState("");

  const [memoLoading, setMemoLoading] = useState(false);

  const [memoSaving, setMemoSaving] = useState(false);

  const [memoMessage, setMemoMessage] = useState("");



  // =========================

  // Summary State

  // =========================



  const [summary, setSummary] = useState("");

  const [summaryLoading, setSummaryLoading] = useState(false);

  const [summaryError, setSummaryError] = useState("");

  const [hasSummary, setHasSummary] = useState(false);



  // =========================

  // Question State

  // =========================

  const [aiTab, setAiTab] = useState<"summary" | "question">("summary");
  const [question, setQuestion] = useState("");
  const [questions, setQuestions] = useState<BookQuestion[]>([]);
  const [questionLoading, setQuestionLoading] = useState(false);
  const [questionSending, setQuestionSending] = useState(false);
  const [questionError, setQuestionError] = useState("");



  // PDF 페이지의 실제 비율을 유지하면서 뷰어의 가로/세로 공간에 맞춘다.
  // 강의 슬라이드(가로형), A4 논문/책(세로형), 페이지마다 비율이 다른 PDF를 모두 고려한다.
  useEffect(() => {
    const pdfArea = pdfAreaRef.current;

    if (!pdfArea || pdfPageSize.width <= 0 || pdfPageSize.height <= 0) {
      return;
    }

    const updatePdfSize = () => {
      const style = window.getComputedStyle(pdfArea);
      const horizontalPadding =
        parseFloat(style.paddingLeft) + parseFloat(style.paddingRight);
      const verticalPadding =
        parseFloat(style.paddingTop) + parseFloat(style.paddingBottom);

      const availableWidth = Math.max(240, pdfArea.clientWidth - horizontalPadding - 8);
      const availableHeight = Math.max(240, pdfArea.clientHeight - verticalPadding - 8);
      const aspectRatio = pdfPageSize.width / pdfPageSize.height;

      // 가로 공간과 세로 공간에 각각 맞췄을 때의 너비 중 작은 값을 사용한다.
      const widthByContainer = availableWidth;
      const widthByHeight = availableHeight * aspectRatio;

      // QHD처럼 큰 화면에서도 지나치게 커지지 않게 상한만 둔다.
      // 작은 화면에서는 최소 너비를 강제하지 않아 PDF가 잘리는 것을 방지한다.
      const fittedWidth = Math.min(widthByContainer, widthByHeight, 1240);

      setPdfWidth(Math.max(240, Math.floor(fittedWidth)));
    };

    updatePdfSize();

    const resizeObserver = new ResizeObserver(updatePdfSize);
    resizeObserver.observe(pdfArea);

    return () => resizeObserver.disconnect();
  }, [pdfPageSize]);

  // =========================
  // Book Load / Progress Save
  // =========================

  useEffect(() => {
    let cancelled = false;

    const loadBook = async () => {
      setBookLoading(true);
      try {
        const response = await fetch(`${API_URL}/${bookId}`);
        if (!response.ok) {
          throw new Error(`도서 조회 실패: ${response.status}`);
        }
        const data: BookInfo = await response.json();
        if (cancelled) return;
        setBook(data);
        if (data.lastReadPage && data.lastReadPage > 0) {
          setPageNumber(data.lastReadPage);
        } else {
          setPageNumber(1);
        }
      } catch (error) {
        console.error("도서 조회 오류:", error);
      } finally {
        if (!cancelled) setBookLoading(false);
      }
    };

    loadBook();
    return () => { cancelled = true; };
  }, [bookId]);

  useEffect(() => {
    if (bookLoading || !book) return;

    const timer = window.setTimeout(async () => {
      try {
        await fetch(`${API_URL}/${bookId}/progress`, {
          method: "PATCH",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ pageNumber }),
        });
      } catch (error) {
        console.error("읽기 진행률 저장 오류:", error);
      }
    }, 250);

    return () => window.clearTimeout(timer);
  }, [bookId, book, bookLoading, pageNumber]);

  // =========================

  // Reading Progress

  // =========================



  const progress =

    numPages > 0

      ? Math.round((pageNumber / numPages) * 100)

      : 0;



  // =========================

  // Page Navigation

  // =========================



  const previousPage = () => {

    setPageNumber((page) =>

      Math.max(1, page - 1)

    );

  };



  const nextPage = () => {

    setPageNumber((page) =>

      Math.min(numPages, page + 1)

    );

  };



  // =========================

  // Memo Load

  // =========================



  useEffect(() => {

    const loadMemo = async () => {

      setMemoLoading(true);

      setMemoMessage("");



      try {

        const response = await fetch(

          `${API_URL}/${bookId}/pages/${pageNumber}/memo`

        );



        if (!response.ok) {

          throw new Error(

            `메모 조회 실패: ${response.status}`

          );

        }



        /*

         * 메모가 없는 경우 백엔드가 null 또는

         * 빈 응답을 반환할 가능성까지 처리한다.

         */

        const text = await response.text();



        if (!text) {

          setMemo("");

          return;

        }



        const data = JSON.parse(text);



        setMemo(

          data?.content ?? ""

        );

      } catch (error) {

        console.error(

          "메모 조회 오류:",

          error

        );



        setMemo("");



        setMemoMessage(

          "메모를 불러오지 못했습니다."

        );

      } finally {

        setMemoLoading(false);

      }

    };



    loadMemo();

  }, [bookId, pageNumber]);



  // =========================

  // Memo Save

  // =========================



  const saveMemo = async () => {

    setMemoSaving(true);

    setMemoMessage("");



    try {

      const response = await fetch(

        `${API_URL}/${bookId}/pages/${pageNumber}/memo`,

        {

          method: "PUT",



          headers: {

            "Content-Type": "application/json",

          },



          body: JSON.stringify({

            content: memo,

          }),

        }

      );



      if (!response.ok) {

        throw new Error(

          `메모 저장 실패: ${response.status}`

        );

      }



      setMemoMessage(

        "저장되었습니다."

      );

    } catch (error) {

      console.error(

        "메모 저장 오류:",

        error

      );



      setMemoMessage(

        "메모 저장에 실패했습니다."

      );

    } finally {

      setMemoSaving(false);

    }

  };



  // =========================

  // Summary Load

  // =========================



  useEffect(() => {

    const loadSummary = async () => {

      /*

       * 페이지가 바뀌었으므로

       * 이전 페이지의 요약을 먼저 제거한다.

       */

      setSummary("");

      setHasSummary(false);

      setSummaryError("");

      setSummaryLoading(true);



      try {

        const response = await fetch(

          `${API_URL}/${bookId}/pages/${pageNumber}/summary`

        );



        if (!response.ok) {

          throw new Error(

            `요약 조회 실패: ${response.status}`

          );

        }



        /*

         * DB에 요약이 없으면 백엔드에서

         * null 또는 빈 응답이 올 수 있으므로

         * response.json()을 바로 사용하지 않는다.

         */

        const text = await response.text();



        if (!text) {

          setSummary("");

          setHasSummary(false);

          return;

        }



        const data = JSON.parse(text);



        if (data?.content) {

          setSummary(data.content);

          setHasSummary(true);

        } else {

          setSummary("");

          setHasSummary(false);

        }

      } catch (error) {

        console.error(

          "요약 조회 오류:",

          error

        );



        setSummary("");

        setHasSummary(false);



        setSummaryError(

          "저장된 요약을 불러오지 못했습니다."

        );

      } finally {

        setSummaryLoading(false);

      }

    };



    loadSummary();

  }, [bookId, pageNumber]);



  // =========================

  // Summary Generate

  // =========================



  const summarizePage = async () => {

    setSummaryLoading(true);

    setSummaryError("");



    try {

      const response = await fetch(

        `${API_URL}/${bookId}/pages/${pageNumber}/summary`,

        {

          method: "POST",

        }

      );



      if (!response.ok) {

        throw new Error(

          `페이지 요약 실패: ${response.status}`

        );

      }



      /*

       * POST 응답은 이제 BookSummary이다.

       *

       * {

       *   id,

       *   content,

       *   model,

       *   createdAt,

       *   updatedAt

       * }

       */

      const data = await response.json();



      setSummary(

        data?.content ??

          "요약 결과가 없습니다."

      );



      setHasSummary(

        Boolean(data?.content)

      );

    } catch (error) {

      console.error(

        "페이지 요약 오류:",

        error

      );



      setSummaryError(

        "페이지 요약을 생성하지 못했습니다."

      );

    } finally {

      setSummaryLoading(false);

    }

  };



  // =========================

  // Question Load

  // =========================

  useEffect(() => {
    const loadQuestions = async () => {
      setQuestionLoading(true);
      setQuestionError("");
      setQuestions([]);
      setQuestion("");

      try {
        const response = await fetch(
          `${API_URL}/${bookId}/pages/${pageNumber}/questions`
        );

        if (!response.ok) {
          throw new Error(`질문 조회 실패: ${response.status}`);
        }

        const text = await response.text();

        if (!text) {
          setQuestions([]);
          return;
        }

        const data = JSON.parse(text);
        setQuestions(Array.isArray(data) ? data : []);
      } catch (error) {
        console.error("질문 조회 오류:", error);
        setQuestions([]);
        setQuestionError("이전 질문을 불러오지 못했습니다.");
      } finally {
        setQuestionLoading(false);
      }
    };

    loadQuestions();
  }, [bookId, pageNumber]);



  // =========================

  // Question Send

  // =========================

  const askQuestion = async () => {
    const trimmedQuestion = question.trim();

    if (!trimmedQuestion) {
      setQuestionError("질문을 입력해주세요.");
      return;
    }

    setQuestionSending(true);
    setQuestionError("");

    try {
      const response = await fetch(
        `${API_URL}/${bookId}/pages/${pageNumber}/questions`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json; charset=utf-8",
          },
          body: JSON.stringify({
            question: trimmedQuestion,
          }),
        }
      );

      if (!response.ok) {
        throw new Error(`질문 요청 실패: ${response.status}`);
      }

      const data: BookQuestion = await response.json();

      setQuestions((previous) => [...previous, data]);
      setQuestion("");
    } catch (error) {
      console.error("질문 요청 오류:", error);
      setQuestionError("AI 답변을 생성하지 못했습니다.");
    } finally {
      setQuestionSending(false);
    }
  };



  // =========================

  // UI

  // =========================



  return (

    <div className={styles.container}>



      {/* =========================

          Header

      ========================= */}



      <header className={styles.header}>



        <div className={styles.logo}>

          Reading Assistant

        </div>



        <div className={styles.bookTitle}>

          {book?.title ?? "PDF Reader"}

        </div>



        <button
          className={styles.backButton}
          onClick={() => router.push("/library")}
        >
          ← 내 서재
        </button>



      </header>



      <main className={styles.reader}>



        {/* =========================

            PDF Viewer

        ========================= */}



        <section className={styles.pdfSection}>



          <div ref={pdfAreaRef} className={styles.pdfArea}>



            <div className={styles.pdfPage}>



              <Document

                key={bookId}
                file={pdfUrl}

                onLoadSuccess={({ numPages }) => {

                  setNumPages(numPages);

                }}

                loading={

                  <div

                    className={

                      styles.pdfLoading

                    }

                  >

                    PDF를 불러오는 중...

                  </div>

                }

                error={

                  <div

                    className={

                      styles.pdfError

                    }

                  >

                    PDF를 불러오지 못했습니다.

                  </div>

                }

              >



                {numPages > 0 &&
                  Array.from(
                    new Set(
                      [pageNumber - 1, pageNumber, pageNumber + 1].filter(
                        (page) => page >= 1 && page <= numPages
                      )
                    )
                  ).map((page) => {
                    const isCurrent = page === pageNumber;

                    return (
                      <div
                        key={`page_${page}`}
                        id={`pdf-page-${page}`}
                        className={
                          isCurrent
                            ? styles.visiblePage
                            : styles.cachedPage
                        }
                        aria-hidden={!isCurrent}
                      >
                        <Page
                          pageNumber={page}
                          width={pdfWidth}
                          renderTextLayer={isCurrent}
                          renderAnnotationLayer={isCurrent}
                          onLoadSuccess={(pdfPage) => {
                            if (!isCurrent) return;
                            const viewport = pdfPage.getViewport({ scale: 1 });
                            setPdfPageSize({
                              width: viewport.width,
                              height: viewport.height,
                            });
                          }}
                        />
                      </div>
                    );
                  })}


              </Document>



            </div>



          </div>



          {/* =========================

              Page Controls

          ========================= */}



          <div className={styles.controls}>



            <button

              className={

                styles.pageButton

              }

              onClick={previousPage}

              disabled={

                pageNumber <= 1

              }

            >

              ← 이전 페이지

            </button>



            <span

              className={

                styles.pageNumber

              }

            >

              {pageNumber} /{" "}

              {numPages || "..."}

            </span>



            <button

              className={

                styles.pageButton

              }

              onClick={nextPage}

              disabled={

                numPages === 0 ||

                pageNumber >= numPages

              }

            >

              다음 페이지 →

            </button>



          </div>



        </section>



        {/* =========================

            Sidebar

        ========================= */}



        <aside className={styles.sidebar}>



          {/* =========================

              Reading Progress

          ========================= */}



          <div

            className={

              styles.sidebarSection

            }

          >



            <h2

              className={

                styles.sidebarTitle

              }

            >

              독서 진행

            </h2>



            <div

              className={

                styles.infoRow

              }

            >



              <span>

                현재 페이지

              </span>



              <strong>

                {pageNumber} /{" "}

                {numPages || "..."}

              </strong>



            </div>



            <div

              className={

                styles.progressBar

              }

            >



              <div

                className={

                  styles.progress

                }

                style={{

                  width: `${progress}%`,

                }}

              />



            </div>



            <div

              className={

                styles.progressText

              }

            >

              {progress}% 읽음

            </div>



          </div>



          {/* =========================

              Page Memo

          ========================= */}



          <div

            className={

              styles.sidebarSection

            }

          >



            <h2

              className={

                styles.sidebarTitle

              }

            >

              페이지 메모

            </h2>



            <textarea

              className={styles.memo}

              placeholder={

                memoLoading

                  ? "메모를 불러오는 중..."

                  : `${pageNumber}페이지에 대한 메모를 작성하세요.`

              }

              value={memo}

              disabled={memoLoading}

              onChange={(e) => {

                setMemo(

                  e.target.value

                );



                setMemoMessage("");

              }}

            />



            <button

              className={

                styles.saveButton

              }

              onClick={saveMemo}

              disabled={

                memoLoading ||

                memoSaving

              }

            >

              {memoSaving

                ? "저장 중..."

                : "메모 저장"}

            </button>



            {memoMessage && (

              <p

                className={

                  styles.memoMessage

                }

              >

                {memoMessage}

              </p>

            )}



          </div>



          {/* =========================

              AI Assistant

          ========================= */}



          <div
            className={`${styles.sidebarSection} ${styles.aiSidebarSection}`}
          >

            <h2
              className={styles.sidebarTitle}
            >
              AI Assistant

            </h2>



            <div className={styles.aiTabs}>
              <button type="button" className={`${styles.aiTab} ${aiTab === "summary" ? styles.aiTabActive : ""}`} onClick={() => setAiTab("summary")}>
                ✨ 페이지 요약
              </button>
              <button type="button" className={`${styles.aiTab} ${aiTab === "question" ? styles.aiTabActive : ""}`} onClick={() => { setAiTab("question"); setQuestionError(""); }}>
                💬 질문하기
              </button>
            </div>

            <div className={styles.aiTabContent}>
              {aiTab === "summary" && (
                <div className={styles.summaryArea}>
                  <button type="button" className={styles.aiButton} onClick={summarizePage} disabled={summaryLoading}>
                    {summaryLoading ? "✨ 불러오는 중..." : hasSummary ? "🔄 요약 다시 생성" : "✨ 이 페이지 요약"}
                  </button>
                  {summaryError && <div className={styles.summaryError}>{summaryError}</div>}
                  {summary && (
                    <div className={styles.summaryBox}>
                      <div className={styles.markdownContent}><ReactMarkdown>{summary}</ReactMarkdown></div>
                    </div>
                  )}
                  {!summary && !summaryLoading && !summaryError && (
                    <div className={styles.aiEmpty}>이 페이지의 핵심 내용을 AI가 정리해줍니다.</div>
                  )}
                </div>
              )}

              {aiTab === "question" && (
                <div className={styles.questionArea}>
                  <div className={styles.questionHistory}>
                    {questionLoading && <p className={styles.questionStatus}>이전 질문을 불러오는 중...</p>}
                    {!questionLoading && questions.length === 0 && (
                      <div className={styles.aiEmpty}>이 페이지에 대해 궁금한 내용을 질문해보세요.</div>
                    )}
                    {!questionLoading && questions.map((item) => (
                      <div key={item.id} className={styles.questionItem}>
                        <div className={styles.userQuestion}>
                          <div className={styles.messageLabel}>나</div>
                          <p>{item.question}</p>
                        </div>
                        <div className={styles.aiAnswer}>
                          <div className={styles.messageLabel}>AI</div>
                          <div className={styles.markdownContent}>
                            <ReactMarkdown>{item.answer}</ReactMarkdown>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>

                  <div className={styles.questionComposer}>
                    {questionError && <div className={styles.questionError}>{questionError}</div>}
                    <textarea
                      className={styles.questionInput}
                      placeholder={`${pageNumber}페이지에 대해 궁금한 점을 질문하세요.`}
                      value={question}
                      disabled={questionSending}
                      onChange={(e) => { setQuestion(e.target.value); setQuestionError(""); }}
                    />
                    <button
                      type="button"
                      className={styles.questionSendButton}
                      onClick={askQuestion}
                      disabled={questionSending || !question.trim()}
                    >
                      {questionSending ? "AI가 답변하는 중..." : "질문 보내기"}
                    </button>
                  </div>
                </div>
              )}
            </div>

          </div>



        </aside>



      </main>



    </div>

  );

}