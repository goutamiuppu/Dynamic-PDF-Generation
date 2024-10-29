package org.assignment.service;


import com.lowagie.text.DocumentException;
import org.assignment.domainmodel.Document;
import org.assignment.exception.PdfGenerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;
import java.nio.file.Files;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PdfGeneratorService {

    @Autowired
    private TemplateEngine templateEngine;

    public static final String PDF_STORAGE_PATH = System.getProperty("user.home") + "/generated_pdfs/";
    private static final String CACHE_INDEX_FILE = PDF_STORAGE_PATH + "cache_index.ser";

    private static final Logger logger = LoggerFactory.getLogger(PdfGeneratorService.class);

    // In-memory cache mapping document hashes to filenames
    private ConcurrentHashMap<String, String> contentHashToFileName;

    public PdfGeneratorService() {
        loadCacheIndex();
    }
    @SuppressWarnings("unchecked")
    private void loadCacheIndex() {
        Path cacheFile = Paths.get(CACHE_INDEX_FILE);
        if (Files.exists(cacheFile)) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(CACHE_INDEX_FILE))) {
                contentHashToFileName = (ConcurrentHashMap<String, String>) ois.readObject();
                logger.info("Loaded invoice cache index with {} entries", contentHashToFileName.size());
            } catch (Exception e) {
                logger.error("Error loading invoice cache index, creating new one", e);
                contentHashToFileName = new ConcurrentHashMap<>();
            }
        } else {
            contentHashToFileName = new ConcurrentHashMap<>();
        }
    }

    private void saveCacheIndex() {
        try {
            Files.createDirectories(Paths.get(PDF_STORAGE_PATH));
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(CACHE_INDEX_FILE))) {
                oos.writeObject(contentHashToFileName);
                logger.info("Saved invoice cache index with {} entries", contentHashToFileName.size());
            }
        } catch (Exception e) {
            logger.error("Error saving invoice cache index", e);
        }
    }

    /**
     * Generates a hash for the document content, including special handling for double values
     * to ensure consistent hashing despite floating-point representation issues.
     */
    private String generateContentHash(Document document) throws PdfGenerationException {
        try {
            StringBuilder contentBuilder = new StringBuilder();
            contentBuilder.append(document.getSellerGstin() != null ? document.getSellerGstin() : "")
                    .append("_")
                    .append(document.getBuyerGstin() != null ? document.getBuyerGstin() : "")
                    .append("_")
                    .append(document.toString());

            // Special handling for double values to ensure consistent hashing
            if (document.getItems() != null) {
                document.getItems().forEach(item -> {
                    contentBuilder.append(String.format("|%.2f:%.2f", item.getRate(), item.getAmount()));
                });
            }

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(contentBuilder.toString().getBytes());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new PdfGenerationException("Failed to generate invoice content hash", e);
        }
    }

    public String generateAndStorePdf(Document document) throws PdfGenerationException {
        try {
            String contentHash = generateContentHash(document);

            // Check if we already have a PDF for this invoice content
            String existingFileName = contentHashToFileName.get(contentHash);
            if (existingFileName != null) {
                Path existingFile = Paths.get(PDF_STORAGE_PATH, existingFileName);
                if (Files.exists(existingFile)) {
                    logger.info("Returning existing invoice PDF file: {}", existingFileName);
                    return existingFileName;
                } else {
                    contentHashToFileName.remove(contentHash);
                }
            }

            // Generate new PDF
            byte[] pdfBytes = generatePdf(document);

            // Create filename using GSTIN for better organization
            String fileName = String.format("%s_%s_%s.pdf",
                    document.getSellerGstin() != null ? document.getSellerGstin() : "noGstin",
                    document.getBuyerGstin() != null ? document.getBuyerGstin() : "noGstin",
                    UUID.randomUUID().toString());

            Path filePath = Paths.get(PDF_STORAGE_PATH, fileName);

            Files.createDirectories(filePath.getParent());
            Files.write(filePath, pdfBytes);

            contentHashToFileName.put(contentHash, fileName);
            saveCacheIndex();

            logger.info("Generated new invoice PDF and stored at: {}", filePath);
            return fileName;
        } catch (IOException e) {
            throw new PdfGenerationException("Failed to generate or store invoice PDF due to IO error", e);
        } catch (Exception e) {
            throw new PdfGenerationException("Unexpected error in invoice PDF generation process", e);
        }
    }

    private byte[] generatePdf(Document document) throws PdfGenerationException {
        try {
            Context context = new Context();
            context.setVariable("document", document);

            String processedHtml = templateEngine.process("document", context);

            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(processedHtml);
            renderer.layout();

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                renderer.createPDF(outputStream);
                return outputStream.toByteArray();
            }
        } catch (Exception e) {
            throw new PdfGenerationException("Failed to generate invoice PDF", e);
        }
    }

    public byte[] retrieveStoredPdf(String fileNameOrPath) throws PdfGenerationException {
        Path filePath;
        if (fileNameOrPath.contains("/")) {
            filePath = Paths.get(fileNameOrPath);
        } else {
            filePath = Paths.get(PDF_STORAGE_PATH, fileNameOrPath);
        }

        if (!Files.exists(filePath)) {
            logger.error("Invoice PDF file not found at: {}", filePath);
            throw new PdfGenerationException("Invoice PDF file not found: " + fileNameOrPath);
        }

        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new PdfGenerationException("Error reading invoice PDF file", e);
        }
    }

    public boolean isValidDocument(Document document) {
        return document != null &&
                document.getSellerGstin() != null && !document.getSellerGstin().trim().isEmpty() &&
                document.getBuyerGstin() != null && !document.getBuyerGstin().trim().isEmpty() &&
                document.getItems() != null && !document.getItems().isEmpty();
    }

}
