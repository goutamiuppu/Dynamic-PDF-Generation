package org.assignment.resources;

import org.assignment.domainmodel.Document;
import org.assignment.service.PdfGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pdf")
public class PdfController {

    @Autowired
    private PdfGeneratorService pdfGeneratorService;

    @PostMapping("/generate")
    public ResponseEntity<String> generateAndStorePdf(@RequestBody Document document) {
        // Validate required fields
        if (!pdfGeneratorService.isValidDocument(document)) {
            return ResponseEntity.badRequest()
                    .body("Invalid document: Required fields are missing");
        }

        try {
            String fileName = pdfGeneratorService.generateAndStorePdf(document);
            return ResponseEntity.ok(fileName);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error generating PDF: " + e.getMessage());
        }
    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable String fileName) {
        try {
            byte[] pdfBytes = pdfGeneratorService.retrieveStoredPdf(fileName);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("filename", fileName);

            return ResponseEntity.ok().headers(headers).body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}