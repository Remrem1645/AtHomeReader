package com.rem.reader.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.rem.reader.DTO.editBookRequestDTO;
import com.rem.reader.Models.Book;
import com.rem.reader.Repo.BookRepo;

@Service
public class BookService {

    private final Path rootDir = Paths.get("data/books");

    @Autowired
    private BookRepo bookRepo;

    public ResponseEntity<?> getBook(UUID uuid) {
        try {
            Book book = bookRepo.findByUuid(uuid);
            if (book == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok().body(Map.of(
                    "uuid", book.getUuid(),
                    "title", book.getTitle(),
                    "author", book.getAuthor(),
                    "description", book.getDescription(),
                    "filePath", book.getFilePath(),
                    "coverImagePath", book.getCoverImagePath()));
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

            if (book.getCoverImagePath() == null || book.getCoverImagePath().isBlank()) {
                return ResponseEntity.notFound().build(); 
            }

            Path coverPath = Paths.get(book.getCoverImagePath());
            var resource = new UrlResource(coverPath.toUri());
            String contentType = Files.probeContentType(coverPath); 

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to retrieve book cover: " + e.getMessage());
        }
    }

    public ResponseEntity<?> getAllBooks() {
        try {
            var books = bookRepo.findAll();
            if (books.isEmpty()) {
                return ResponseEntity.noContent().build();
            }

            var response = books.stream().map(book -> Map.of(
                    "uuid", book.getUuid(),
                    "title", book.getTitle(),
                    "author", book.getAuthor(),
                    "coverImagePath", book.getCoverImagePath()));

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
            if (metadata.get("title") != null)
                book.setTitle(metadata.get("title"));
            if (metadata.get("author") != null)
                book.setAuthor(metadata.get("author"));
            if (metadata.get("description") != null)
                book.setDescription(metadata.get("description"));

            Path coverImagePath = extractCoverImage(epubPath, bookFolder);
            if (coverImagePath != null) {
                book.setCoverImagePath(coverImagePath.toString());
            }

            bookRepo.save(book);

            return ResponseEntity.ok().body(Map.of(
                    "uuid", uuid,
                    "title", book.getTitle(),
                    "author", book.getAuthor(),
                    "filePath", book.getFilePath(),
                    "coverImagePath", book.getCoverImagePath()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to upload book: " + e.getMessage());
        }
    }

    public ResponseEntity<?> editBook(UUID uuid, editBookRequestDTO editBookRequest) {
        try {
            Book book = bookRepo.findByUuid(uuid);
            if (book == null) {
                return ResponseEntity.notFound().build();
            }

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
            if (book == null) {
                return ResponseEntity.notFound().build();
            }

            // Delete the book file and cover image if they exist
            Path bookPath = Paths.get(book.getFilePath());
            if (Files.exists(bookPath)) {
                Files.delete(bookPath);
            }

            if (book.getCoverImagePath() != null && !book.getCoverImagePath().isBlank()) {
                Path coverPath = Paths.get(book.getCoverImagePath());
                if (Files.exists(coverPath)) {
                    Files.delete(coverPath);
                }
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

    private Path extractCoverImage(Path epubPath, Path bookFolder) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(epubPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName().toLowerCase();
                if ((name.contains("cover") || name.contains("cover-image")) &&
                        (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png"))) {

                    String ext = name.substring(name.lastIndexOf("."));
                    Path coverPath = bookFolder.resolve("cover" + ext);
                    Files.copy(zis, coverPath, StandardCopyOption.REPLACE_EXISTING);
                    return coverPath;
                }
            }
        }
        return null;
    }

    private static String firstNonNull(Metadata meta, String... keys) {
        for (String key : keys) {
            String value = meta.get(key);
            if (value != null && !value.isBlank())
                return value;
        }
        return null;
    }
}
