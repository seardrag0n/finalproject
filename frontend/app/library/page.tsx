"use client";

import { ChangeEvent, FormEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import styles from "./library.module.css";

const API_URL = "http://localhost:8080/api/books";

type Book = {
  id: number;
  title: string;
  author?: string | null;
  genre?: string | null;
  originalFilename?: string | null;
  totalPages?: number | null;
  lastReadPage?: number | null;
};

export default function LibraryPage() {
  const router = useRouter();
  const [books, setBooks] = useState<Book[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [uploadOpen, setUploadOpen] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [file, setFile] = useState<File | null>(null);
  const [title, setTitle] = useState("");
  const [author, setAuthor] = useState("");
  const [genre, setGenre] = useState("");

  const loadBooks = async () => {
    setLoading(true);
    setError("");
    try {
      const response = await fetch(API_URL);
      if (!response.ok) throw new Error(`서재 조회 실패: ${response.status}`);
      const data = await response.json();
      setBooks(Array.isArray(data) ? data : []);
    } catch (e) {
      console.error(e);
      setError("내 서재를 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadBooks();
  }, []);

  const handleFile = (event: ChangeEvent<HTMLInputElement>) => {
    const selected = event.target.files?.[0] ?? null;
    setFile(selected);
    if (selected && !title.trim()) {
      setTitle(selected.name.replace(/\.pdf$/i, ""));
    }
  };

  const uploadBook = async (event: FormEvent) => {
    event.preventDefault();
    if (!file || !title.trim()) {
      setError("PDF 파일과 제목을 입력해주세요.");
      return;
    }

    setUploading(true);
    setError("");
    try {
      const form = new FormData();
      form.append("title", title.trim());
      if (author.trim()) form.append("author", author.trim());
      if (genre.trim()) form.append("genre", genre.trim());
      form.append("file", file);

      const response = await fetch(`${API_URL}/upload`, {
        method: "POST",
        body: form,
      });
      if (!response.ok) throw new Error(`PDF 업로드 실패: ${response.status}`);

      const saved: Book = await response.json();
      setUploadOpen(false);
      setFile(null);
      setTitle("");
      setAuthor("");
      setGenre("");
      await loadBooks();
      router.push(`/reader/${saved.id}`);
    } catch (e) {
      console.error(e);
      setError("PDF를 업로드하지 못했습니다.");
    } finally {
      setUploading(false);
    }
  };

  return (
    <main className={styles.page}>
      <header className={styles.header}>
        <div>
          <h1>Reading Assistant</h1>
          <p>읽던 PDF를 이어서 보거나 새로운 문서를 추가하세요.</p>
        </div>
        <button className={styles.primaryButton} onClick={() => setUploadOpen(true)}>
          + PDF 추가
        </button>
      </header>

      {error && <div className={styles.error}>{error}</div>}

      {loading ? (
        <div className={styles.empty}>서재를 불러오는 중...</div>
      ) : books.length === 0 ? (
        <div className={styles.empty}>
          <strong>아직 등록된 PDF가 없습니다.</strong>
          <span>PDF를 추가하면 페이지별 메모, 요약, 질문 기능을 사용할 수 있습니다.</span>
          <button className={styles.primaryButton} onClick={() => setUploadOpen(true)}>
            첫 PDF 추가하기
          </button>
        </div>
      ) : (
        <section className={styles.grid}>
          {books.map((book) => {
            const total = book.totalPages ?? 0;
            const current = Math.min(book.lastReadPage ?? 1, total || 1);
            const progress = total > 0 ? Math.round((current / total) * 100) : 0;

            return (
              <button
                key={book.id}
                className={styles.card}
                onClick={() => router.push(`/reader/${book.id}`)}
              >
                <div className={styles.preview}>
                  <span>PDF</span>
                </div>
                <div className={styles.cardBody}>
                  <h2>{book.title}</h2>
                  <p>{book.author || book.originalFilename || "저자 정보 없음"}</p>
                  <div className={styles.meta}>
                    <span>{total ? `${current} / ${total} 페이지` : "페이지 정보 없음"}</span>
                    <span>{progress}%</span>
                  </div>
                  <div className={styles.progressBar}>
                    <div className={styles.progress} style={{ width: `${progress}%` }} />
                  </div>
                </div>
              </button>
            );
          })}
        </section>
      )}

      {uploadOpen && (
        <div className={styles.overlay} onMouseDown={() => !uploading && setUploadOpen(false)}>
          <form className={styles.modal} onSubmit={uploadBook} onMouseDown={(e) => e.stopPropagation()}>
            <div className={styles.modalHeader}>
              <h2>PDF 추가</h2>
              <button type="button" onClick={() => setUploadOpen(false)} disabled={uploading}>×</button>
            </div>

            <label>
              PDF 파일
              <input type="file" accept="application/pdf,.pdf" onChange={handleFile} disabled={uploading} />
            </label>
            <label>
              제목
              <input value={title} onChange={(e) => setTitle(e.target.value)} disabled={uploading} />
            </label>
            <label>
              저자 <span>(선택)</span>
              <input value={author} onChange={(e) => setAuthor(e.target.value)} disabled={uploading} />
            </label>
            <label>
              장르 <span>(선택)</span>
              <input value={genre} onChange={(e) => setGenre(e.target.value)} disabled={uploading} />
            </label>

            <div className={styles.modalActions}>
              <button type="button" className={styles.secondaryButton} onClick={() => setUploadOpen(false)} disabled={uploading}>취소</button>
              <button type="submit" className={styles.primaryButton} disabled={uploading || !file || !title.trim()}>
                {uploading ? "업로드 및 텍스트 추출 중..." : "추가하고 읽기"}
              </button>
            </div>
          </form>
        </div>
      )}
    </main>
  );
}
