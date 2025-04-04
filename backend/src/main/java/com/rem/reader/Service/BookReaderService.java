package com.rem.reader.Service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.rem.reader.Models.Book;
import com.rem.reader.Models.BookPageCache;

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
    BookService bookService;

    @Autowired
    ProgressService progressService;

    @Autowired
    BookPageCacheService bookPageCacheService;

    private static final int BLOCKS_PER_PAGE = 15;

    // Public methods

    /**
     * Retrieves the pages of a book based on the book's UUID and the requested page number.
     * If the book's pages are not cached, it extracts and caches them.
     * Updates the user's current page in the progress service.
     * 
     * @param bookUuid   The UUID of the book.
     * @param pageNumber The requested page number.
     * @param session    The HTTP session.
     * @return A ResponseEntity containing the book pages or an error message.
     */
    public ResponseEntity<?> getBookPages(UUID bookUuid, int pageNumber, HttpSession session) {
        try {
            Book book = bookService.getBookObjectById(bookUuid);
            if (book == null)
                return ResponseEntity.notFound().build();

            Path epubPath = Paths.get(book.getFilePath());
            if (!bookPageCacheService.existsByBookId(bookUuid))
                extractAndCachePages(epubPath, bookUuid);

            Page<BookPageCache> page = bookPageCacheService.getBookPageCacheByBookId(
                    bookUuid,
                    pageNumber,
                    1);

            if (page.isEmpty())
                return ResponseEntity.ok(Collections.emptyList());

            UUID userUuid = (UUID) session.getAttribute("userUuid");
            progressService.updateUserCurrentPage(userUuid, bookUuid, pageNumber);

            return ResponseEntity.ok(page.getContent());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to retrieve book pages: " + e.getMessage());
        }
    }


    /**
     * Retrieves the current book asset based on the book's UUID and the asset path.
     * 
     * @param bookUuid  The UUID of the book.
     * @param assetPath The path of the asset.
     * @return A ResponseEntity containing the asset or an error message.
     */
    public ResponseEntity<?> getCurrentBookAsset(UUID bookUuid, String assetPath) {
        try {
            Book book = bookService.getBookObjectById(bookUuid);
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

    // Internal methods

    /**
     * Deletes all progress entries for a specific book UUID.
     * 
     * @param bookUuid The UUID of the book for which to remove progress entries.
     */
    public void deleteAllProgressByBoodId(UUID bookUuid) {
        progressService.removeAllProggressByBookId(bookUuid);
    }

    // Private methods

    /**
     * Extracts and caches the pages of a book from the EPUB file.
     * 
     * @param epubPath The path to the EPUB file.
     * @param bookUuid The UUID of the book.
     * @throws IOException If an I/O error occurs during extraction or caching.
     */
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
                            String newSrc = "http://localhost:8080/api/book-reader/" + bookUuid + "/assets/"
                                    + imageName;
                            img.attr("src", newSrc);
                        });

                        boolean isFullImage = cloned.tagName().equals("p")
                                && cloned.childrenSize() == 1
                                && cloned.select("img").size() == 1;

                        if (isFullImage) {
                            if (!pageBuilder.isEmpty()) {
                                String htmlPage = pageBuilder.toString().trim();
                                pagesToCache.add(createCache(bookUuid, pageCounter++, chapterTitle, htmlPage));
                                pageBuilder = new StringBuilder();
                                blockCount = 0;
                            }
                            String imagePage = cloned.outerHtml();
                            pagesToCache.add(createCache(bookUuid, pageCounter++, chapterTitle, imagePage));
                            continue;
                        }

                        pageBuilder.append(cloned.outerHtml()).append("\n");
                        blockCount++;

                        if (blockCount >= BLOCKS_PER_PAGE) {
                            String htmlPage = pageBuilder.toString().trim();
                            pagesToCache.add(createCache(bookUuid, pageCounter++, chapterTitle, htmlPage));
                            pageBuilder = new StringBuilder();
                            blockCount = 0;
                        }
                    }

                    if (!pageBuilder.isEmpty()) {
                        String htmlPage = pageBuilder.toString().trim();
                        pagesToCache.add(createCache(bookUuid, pageCounter++, chapterTitle, htmlPage));
                    }
                }
            }
        }
        bookService.updatePagesByUuid(bookUuid, pageCounter);
        bookPageCacheService.saveBookPageCache(pagesToCache);
    }

    /**
     * Creates a BookPageCache object with the specified parameters.
     * 
     * @param bookUuid  The UUID of the book.
     * @param pageNumber The page number.
     * @param title     The title of the page.
     * @param content   The content of the page.
     * @return A BookPageCache object.
     */
    private BookPageCache createCache(UUID bookUuid, int pageNumber, String title, String content) {
        BookPageCache page = new BookPageCache();
        page.setBookId(bookUuid);
        page.setPageNumber(pageNumber);
        page.setTitle(title);
        page.setContent(content);
        return page;
    }
}
