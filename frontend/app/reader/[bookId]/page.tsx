"use client";

import { useParams } from "next/navigation";
import PdfViewer from "../PdfViewer";

export default function ReaderPage() {
  const params = useParams<{ bookId: string }>();
  const bookId = Number(params.bookId);

  if (!Number.isFinite(bookId) || bookId < 1) {
    return <main style={{ padding: 32 }}>잘못된 도서 번호입니다.</main>;
  }

  return <PdfViewer bookId={bookId} />;
}
