package com.rem.reader.Service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.rem.reader.Models.Book;
import com.rem.reader.Repo.BookRepo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class BookReaderService {

    @Autowired
    BookRepo bookRepo;

    private static final int BLOCKS_PER_PAGE = 15;

    public static class BookPage {
        public int pageNumber;
        public String title;
        public String contentHtml;

        public BookPage(int pageNumber, String title, String contentHtml) {
            this.pageNumber = pageNumber;
            this.title = title;
            this.contentHtml = contentHtml;
        }
    }

    public ResponseEntity<?> getBookPages(UUID uuid, int pageNumber, int pageSize) {
        try {
            Book book = bookRepo.findByUuid(uuid);
            if (book == null) {
                return ResponseEntity.notFound().build();
            }

            Path epubPath = Paths.get(book.getFilePath());
            List<BookPage> pages = extractPages(epubPath, uuid);

            int start = pageNumber * pageSize;
            int end = Math.min(start + pageSize, pages.size());

            if (start >= pages.size()) {
                return ResponseEntity.ok(Collections.emptyList());
            }
            return ResponseEntity.ok(pages.subList(start, end));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to retrieve book pages: " + e.getMessage());
        }
    }

    public ResponseEntity<?> getCurrentBookAsset(UUID uuid, String assetPath) {
        try {
            Book book = bookRepo.findByUuid(uuid);
            if (book == null) {
                return ResponseEntity.notFound().build();
            }

            Path imagePath = Paths.get(book.getFilePath()).getParent().resolve("assets").resolve(assetPath);

            UrlResource image = new UrlResource(imagePath.toUri());
            String contentType = Files.probeContentType(imagePath);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(image);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to retrieve asset: " + e.getMessage());
        }
    }

    public List<BookPage> extractPages(Path epubPath, UUID bookUuid) throws IOException {
        List<BookPage> pages = new ArrayList<>();
        int pageCounter = 0;
        Path bookDir = epubPath.getParent();
        Path assetsDir = bookDir.resolve("assets");
        Files.createDirectories(assetsDir);

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(epubPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                String lowerName = name.toLowerCase();

                if (lowerName.matches(".*\\.(jpg|jpeg|png|gif|svg)$")) {
                    Path imagePath = assetsDir.resolve(Path.of(name).getFileName().toString());
                    Files.copy(zis, imagePath, StandardCopyOption.REPLACE_EXISTING);
                    continue;
                }

                if ((lowerName.endsWith(".xhtml") || lowerName.endsWith(".html")) && !lowerName.contains("toc")) {
                    byte[] contentBytes = zis.readAllBytes();
                    String html = new String(contentBytes, StandardCharsets.UTF_8);

                    Document doc = Jsoup.parse(html);
                    String chapterTitle = doc.title();
                    Elements blocks = doc.select("h1, h2, h3, p, blockquote");

                    StringBuilder pageBuilder = new StringBuilder();
                    int blockCount = 0;

                    for (Element block : blocks) {
                        Element cloned = block.clone();
                        cloned.select("img").forEach(img -> {
                            String originalSrc = img.attr("src");
                            String imageName = Path.of(originalSrc).getFileName().toString();
                            String newSrc = "/api/books/" + bookUuid + "/assets/" + imageName;
                            img.attr("src", newSrc);
                        });

                        pageBuilder.append(cloned.outerHtml()).append("\n");
                        blockCount++;

                        if (blockCount >= BLOCKS_PER_PAGE) {
                            pages.add(new BookPage(++pageCounter, chapterTitle, pageBuilder.toString().trim()));
                            pageBuilder = new StringBuilder();
                            blockCount = 0;
                        }
                    }

                    if (!pageBuilder.isEmpty()) {
                        pages.add(new BookPage(++pageCounter, chapterTitle, pageBuilder.toString().trim()));
                    }
                }
            }
        }
        return pages;
    }
}
