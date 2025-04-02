package com.rem.reader.Service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.rem.reader.Models.Book;
import com.rem.reader.Models.BookPageCache;
import com.rem.reader.Repo.BookPageCacheRepo;
import com.rem.reader.Repo.BookRepo;

import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class BookReaderService {

    @Autowired
    BookRepo bookRepo;

    @Autowired
    ProgressService progressService;

    @Autowired
    BookPageCacheRepo bookPageCacheRepo;

    private static final int BLOCKS_PER_PAGE = 25;

    public ResponseEntity<?> getBookPages(UUID bookUuid, int pageNumber, HttpSession session) {
        try {
            Book book = bookRepo.findByUuid(bookUuid);
            if (book == null) {
                return ResponseEntity.notFound().build();
            }

            Path epubPath = Paths.get(book.getFilePath());
            if (!bookPageCacheRepo.existsByBookId(bookUuid)) {
                extractAndCachePages(epubPath, bookUuid);
            }

            Pageable pageable = PageRequest.of(pageNumber, 1);
            Page<BookPageCache> page = bookPageCacheRepo.findByBookIdOrderByPageNumberAsc(bookUuid, pageable);

            if (page.isEmpty()) {
                return ResponseEntity.ok(Collections.emptyList());
            }

            UUID userUuid = (UUID) session.getAttribute("userUuid");
            progressService.updateUserCurrentPage(userUuid, bookUuid, pageNumber);

            return ResponseEntity.ok(page.getContent());

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

    private void extractAndCachePages(Path epubPath, UUID bookUuid) throws IOException {
        List<BookPageCache> pagesToCache = new ArrayList<>();
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
                            String newSrc = "http://localhost:8080/api/book-reader/" + bookUuid + "/assets/" + imageName;
                            img.attr("src", newSrc);
                        });

                        boolean isFullImage = cloned.tagName().equals("p")
                                && cloned.childrenSize() == 1
                                && cloned.select("img").size() == 1;

                        if (isFullImage) {
                            if (!pageBuilder.isEmpty()) {
                                String htmlPage = pageBuilder.toString().trim();
                                pagesToCache.add(createCache(bookUuid, ++pageCounter, chapterTitle, htmlPage));
                                pageBuilder = new StringBuilder();
                                blockCount = 0;
                            }
                            String imagePage = cloned.outerHtml();
                            pagesToCache.add(createCache(bookUuid, ++pageCounter, chapterTitle, imagePage));
                            continue;
                        }

                        pageBuilder.append(cloned.outerHtml()).append("\n");
                        blockCount++;

                        if (blockCount >= BLOCKS_PER_PAGE) {
                            String htmlPage = pageBuilder.toString().trim();
                            pagesToCache.add(createCache(bookUuid, ++pageCounter, chapterTitle, htmlPage));
                            pageBuilder = new StringBuilder();
                            blockCount = 0;
                        }
                    }

                    if (!pageBuilder.isEmpty()) {
                        String htmlPage = pageBuilder.toString().trim();
                        pagesToCache.add(createCache(bookUuid, ++pageCounter, chapterTitle, htmlPage));
                    }
                }
            }
        }

        bookPageCacheRepo.saveAll(pagesToCache);
    }

    private BookPageCache createCache(UUID bookUuid, int pageNumber, String title, String content) {
        BookPageCache page = new BookPageCache();
        page.setBookId(bookUuid);
        page.setPageNumber(pageNumber);
        page.setTitle(title);
        page.setContent(content);
        return page;
    }
}
