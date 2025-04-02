package com.rem.reader.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.bouncycastle.oer.its.etsi102941.Url;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.rem.reader.DTO.EditBookRequestDTO;
import com.rem.reader.Models.Book;
import com.rem.reader.Models.Progress;
import com.rem.reader.Repo.BookRepo;

import jakarta.servlet.http.HttpSession;

@Service
public class BookService {

    private final Path rootDir = Paths.get("data/books");

    @Autowired
    private BookRepo bookRepo;

    @Autowired
    private ProgressService progressService;

    public ResponseEntity<?> getBook(UUID uuid, HttpSession session) {
        try {
            Book book = bookRepo.findByUuid(uuid);
            if (book == null) return ResponseEntity.notFound().build();

            UUID userUuid = (UUID) session.getAttribute("userUuid");
            Progress progress = progressService.getUserProgress(userUuid, uuid);

            String coverUrl = book.getCoverImagePath() != null
                    ? "http://localhost:8080/api/books/" + book.getUuid() + "/cover"
                    : null;

            return ResponseEntity.ok().body(Map.of(
                    "uuid", book.getUuid(),
                    "title", book.getTitle(),
                    "author", book.getAuthor(),
                    "currentPage", progress != null ? progress.getCurrentPageNumber() : 0,
                    "description", book.getDescription(),
                    "coverImageUrl", coverUrl
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to retrieve book: " + e.getMessage());
        }
    }

    public ResponseEntity<?> getBookCover(UUID uuid) {
        try {
            Book book = bookRepo.findByUuid(uuid);
            if (book == null) {
                return ResponseEntity.notFound().build();
            }
    
            // Resolve the expected cover path (even if it's null)
            Path coverPath = book.getCoverImagePath() != null
                    ? Paths.get(book.getFilePath()).getParent().resolve(book.getCoverImagePath())
                    : null;
    
            Path imagePath;
            if (coverPath != null && Files.exists(coverPath) && Files.isReadable(coverPath)) {
                imagePath = coverPath;
            } else {
                // Fallback to default cover
                imagePath = Paths.get("data/noCover.png");
                if (!Files.exists(imagePath)) {
                    return ResponseEntity.notFound().build(); 
                }
            }
    
            UrlResource resource = new UrlResource(imagePath.toUri());
            String contentType = Files.probeContentType(imagePath);
    
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType != null ? contentType : "image/png"))
                    .body(resource);
    
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to retrieve book cover: " + e.getMessage());
        }
    }
    
    public ResponseEntity<?> getAllBooks(HttpSession session) {
        try {
            var books = bookRepo.findAll();
            if (books.isEmpty()) return ResponseEntity.noContent().build();

            UUID userUuid = (UUID) session.getAttribute("userUuid");

            var response = books.stream().map(book -> {
                String coverUrl = "http://localhost:8080/api/books/" + book.getUuid() + "/cover";

                return Map.of(
                        "uuid", book.getUuid(),
                        "title", book.getTitle(),
                        "author", book.getAuthor(),
                        "currentPage", Optional.ofNullable(progressService.getUserProgress(userUuid, book.getUuid()))
                                .map(Progress::getCurrentPageNumber).orElse(0),
                        "coverImageUrl", coverUrl
                );
            });

            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to retrieve books: " + e.getMessage());
        }
    }

    public ResponseEntity<?> uploadBook(MultipartFile file) {
        try {
            UUID uuid = UUID.randomUUID();
            Book book = new Book();
            book.setUuid(uuid);

            Path bookFolder = rootDir.resolve(uuid.toString());
            Files.createDirectories(bookFolder);

            Path epubPath = bookFolder.resolve(file.getOriginalFilename());
            Files.copy(file.getInputStream(), epubPath, StandardCopyOption.REPLACE_EXISTING);
            book.setFilePath(epubPath.toString());

            Map<String, String> metadata = extractMetaData(epubPath);
            if (metadata.get("title") != null) book.setTitle(metadata.get("title"));
            if (metadata.get("author") != null) book.setAuthor(metadata.get("author"));
            if (metadata.get("description") != null) book.setDescription(metadata.get("description"));

            String coverImageFilename = extractCoverImage(epubPath, bookFolder);
            if (coverImageFilename != null) {
                book.setCoverImagePath(coverImageFilename);
            }

            bookRepo.save(book);

            String coverUrl = book.getCoverImagePath() != null
                    ? "http://localhost:8080/api/books/" + uuid + "/cover"
                    : "";

            return ResponseEntity.ok().body(Map.of(
                    "uuid", uuid,
                    "title", book.getTitle(),
                    "author", book.getAuthor(),
                    "coverImageUrl", coverUrl
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to upload book: " + e.getMessage());
        }
    }

    public ResponseEntity<?> editBook(UUID uuid, EditBookRequestDTO editBookRequest, HttpSession session) {
        try {
            Book book = bookRepo.findByUuid(uuid);
            if (book == null) return ResponseEntity.notFound().build();

            if (editBookRequest.getTitle() != null && !editBookRequest.getTitle().isBlank()) {
                book.setTitle(editBookRequest.getTitle());
            }
            if (editBookRequest.getAuthor() != null && !editBookRequest.getAuthor().isBlank()) {
                book.setAuthor(editBookRequest.getAuthor());
            }
            if (editBookRequest.getDescription() != null && !editBookRequest.getDescription().isBlank()) {
                book.setDescription(editBookRequest.getDescription());
            }

            bookRepo.save(book);
            return ResponseEntity.ok().body("Book updated successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to update book: " + e.getMessage());
        }
    }

    public ResponseEntity<?> deleteBook(UUID uuid) {
        try {
            Book book = bookRepo.findByUuid(uuid);
            if (book == null) return ResponseEntity.notFound().build();

            Path bookPath = Paths.get(book.getFilePath());
            if (Files.exists(bookPath)) Files.delete(bookPath);

            if (book.getCoverImagePath() != null && !book.getCoverImagePath().isBlank()) {
                Path coverPath = Paths.get(book.getFilePath())
                        .getParent()
                        .resolve(book.getCoverImagePath());
                if (Files.exists(coverPath)) Files.delete(coverPath);
            }

            bookRepo.delete(book);
            return ResponseEntity.ok().body("Book deleted successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to delete book: " + e.getMessage());
        }
    }

    private static Map<String, String> extractMetaData(Path filePath) throws IOException, TikaException, SAXException {
        Metadata metadata = new Metadata();
        try (InputStream stream = Files.newInputStream(filePath)) {
            AutoDetectParser parser = new AutoDetectParser();
            parser.parse(stream, new DefaultHandler(), metadata, new ParseContext());
        }

        Map<String, String> extracted = new HashMap<>();
        extracted.put("title", firstNonNull(metadata, "title", "dc:title", "meta:title"));
        extracted.put("author", firstNonNull(metadata, "creator", "dc:creator", "meta:author"));
        extracted.put("description", firstNonNull(metadata, "description", "dc:description", "meta:description"));
        return extracted;
    }

    private static String firstNonNull(Metadata meta, String... keys) {
        for (String key : keys) {
            String value = meta.get(key);
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private String extractCoverImage(Path epubPath, Path bookFolder) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(epubPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName().toLowerCase();

                if ((name.contains("cover") || name.contains("cover-image")) &&
                        (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png"))) {

                    String ext = name.substring(name.lastIndexOf("."));
                    String imageName = "cover" + ext;
                    Path coverPath = bookFolder.resolve(imageName);
                    Files.copy(zis, coverPath, StandardCopyOption.REPLACE_EXISTING);
                    return imageName;
                }
            }
        }
        return null;
    }
}