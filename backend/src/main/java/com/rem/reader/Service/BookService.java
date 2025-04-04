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

    @Autowired
    private BookRepo bookRepo;

    @Autowired
    private ProgressService progressService;

    @Autowired
    private BookReaderService bookReaderService;

    @Autowired
    private BookPageCacheService bookPageCacheService;

    private final Path rootDir = Paths.get("data/books");

    // Public methods

    /**
     * Retrieves a book by its UUID and returns its details along with the user's progress.
     * 
     * @param uuid The UUID of the book to retrieve.
     * @param session The HTTP session containing user information.
     * @return A ResponseEntity containing the book details and user's progress, or an error message.
     */
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

    /**
     * Retrieves the cover image of a book by its UUID.
     * 
     * @param uuid The UUID of the book.
     * @return A ResponseEntity containing the cover image or an error message.
     */
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
    
    /**
     * Retrieves all books from the database and returns their details along with the user's progress.
     * 
     * @param session The HTTP session containing user information.
     * @return A ResponseEntity containing the list of books and their details, or an error message.
     */
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

    /**
     * Uploads a book file, extracts its metadata, and saves it to the database.
     * 
     * @param file The book file to upload.
     * @return A ResponseEntity containing the book details or an error message.
     */
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

    /** 
     * Edits the details of a book based on the provided UUID and request data.
     * 
     * @param uuid The UUID of the book to edit.
     * @param editBookRequest The request data containing the new book details.
     * @param session The HTTP session containing user information.
     * @return A ResponseEntity indicating the success or failure of the operation.
     */
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

    /**
     * Deletes a book and its associated files from the server.
     * 
     * @param uuid The UUID of the book to delete.
     * @return A ResponseEntity indicating the success or failure of the operation.
     */
    public ResponseEntity<?> deleteBook(UUID uuid) {
        try {
            Book book = bookRepo.findByUuid(uuid);
            if (book == null) return ResponseEntity.notFound().build();

            Path bookFolder = Paths.get(book.getFilePath()).getParent();

            if (Files.exists(bookFolder)) {
                Files.walk(bookFolder)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
            }

            bookRepo.delete(book);
            bookReaderService.deleteAllProgressByBoodId(uuid);
            bookPageCacheService.deleteBookPageCacheByBookId(uuid);
            return ResponseEntity.ok().body("Book deleted successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to delete book: " + e.getMessage());
        }
    }

    // Internal methods

    /**
     * Internal method to retrieve a book object by its UUID.
     * 
     * @param uuid The UUID of the book.
     * @return The Book object associated with the given UUID.
     */
    public Book getBookObjectById(UUID uuid) {
        return bookRepo.findByUuid(uuid);
    }

    /**
     * Internal method to update the number of pages in a book by its UUID.
     * 
     * @param uuid The UUID of the book to update.
     * @param pages The new number of pages.
     */
    public void updatePagesByUuid(UUID uuid, int pages) {
        bookRepo.updatePagesByUuid(uuid, pages);
    }

    // Private methods

    /**
     * Extracts metadata from an EPUB file.
     * 
     * @param filePath The path to the EPUB file.
     * @return A map containing the extracted metadata.
     * @throws IOException If an I/O error occurs.
     * @throws TikaException If a Tika error occurs.
     * @throws SAXException If a SAX error occurs.
     */
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

    /**
     * Returns the first non-null value from the metadata for the given keys.
     * 
     * @param meta The metadata object.
     * @param keys The keys to check in the metadata.
     * @return The first non-null value found, or null if none are found.
     */
    private static String firstNonNull(Metadata meta, String... keys) {
        for (String key : keys) {
            String value = meta.get(key);
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    /**
     * Extracts the cover image from an EPUB file and saves it to the specified folder.
     * 
     * @param epubPath The path to the EPUB file.
     * @param bookFolder The folder to save the cover image.
     * @return The name of the cover image file, or null if not found.
     * @throws IOException If an I/O error occurs.
     */
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