package org.assignment.service;

import org.assignment.domainmodel.Document;
import org.assignment.domainmodel.Item;
import org.assignment.exception.PdfGenerationException;
import org.assignment.mockdata.MockDocumentData;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PdfGeneratorServiceTest {
    private static final Logger logger = LoggerFactory.getLogger(PdfGeneratorServiceTest.class);

    @Autowired
    private PdfGeneratorService pdfGeneratorService;

    private MockDocumentData mockData;

    @Before
    public void setUp() throws IOException {
        Files.createDirectories(Paths.get(PdfGeneratorService.PDF_STORAGE_PATH));
        mockData = new MockDocumentData();
        cleanUp(); // Start with a clean directory
    }

    @After
    public void cleanUp() throws IOException {
        Files.walk(Paths.get(PdfGeneratorService.PDF_STORAGE_PATH))
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @Test
    public void testGenerateAndStorePdf() throws PdfGenerationException, IOException {
        Document document = mockData.createSampleDocument();
        String fileName = pdfGeneratorService.generateAndStorePdf(document);

        assertNotNull("Filename should not be null", fileName);
        assertTrue("Filename should contain seller GSTIN", fileName.contains(document.getSellerGstin()));

        Path filePath = Paths.get(PdfGeneratorService.PDF_STORAGE_PATH, fileName);
        logger.info("File path returned: {}", filePath);

        assertTrue("Generated PDF file should exist: " + filePath, Files.exists(filePath));
        assertTrue("Generated PDF file should not be empty", Files.size(filePath) > 0);
    }

    @Test
    public void testRetrieveStoredPdf() throws PdfGenerationException {
        Document document = mockData.createSampleDocument();
        String fileName = pdfGeneratorService.generateAndStorePdf(document);

        byte[] pdfBytes = pdfGeneratorService.retrieveStoredPdf(fileName);
        assertNotNull("Retrieved PDF bytes should not be null", pdfBytes);
        assertTrue("Retrieved PDF bytes should not be empty", pdfBytes.length > 0);

        Path filePath = Paths.get(PdfGeneratorService.PDF_STORAGE_PATH, fileName);
        assertTrue("Generated PDF file should exist: " + filePath, Files.exists(filePath));
    }

    @Test
    public void testCachingForIdenticalDocuments() throws PdfGenerationException {
        Document document1 = mockData.createSampleDocument();
        Document document2 = mockData.createSampleDocument(); // Create identical document

        String fileName1 = pdfGeneratorService.generateAndStorePdf(document1);
        String fileName2 = pdfGeneratorService.generateAndStorePdf(document2);

        assertEquals("Identical documents should return same filename", fileName1, fileName2);
    }

    @Test
    public void testDifferentDocumentsGenerateDifferentFiles() throws PdfGenerationException {
        Document document1 = mockData.createSampleDocument();
        Document document2 = mockData.createSampleDocument();
        document2.setBuyerGstin("DIFFERENT_GSTIN");

        String fileName1 = pdfGeneratorService.generateAndStorePdf(document1);
        String fileName2 = pdfGeneratorService.generateAndStorePdf(document2);

        assertNotEquals("Different documents should generate different files", fileName1, fileName2);
    }

    @Test(expected = PdfGenerationException.class)
    public void testRetrieveNonExistentPdf() throws PdfGenerationException {
        pdfGeneratorService.retrieveStoredPdf("non_existent_file.pdf");
    }

    @Test
    public void testGeneratePdfWithEmptyItems() throws PdfGenerationException {
        Document document = mockData.createSampleDocument();
        document.setItems(new ArrayList<>());

        String fileName = pdfGeneratorService.generateAndStorePdf(document);
        assertNotNull(fileName);

        Path filePath = Paths.get(PdfGeneratorService.PDF_STORAGE_PATH, fileName);
        assertTrue(Files.exists(filePath));
    }

    @Test
    public void testGeneratePdfWithNullFields() throws PdfGenerationException {
        Document document = new Document();
        document.setSellerGstin("TEST_GSTIN");
        document.setBuyerGstin("BUYER_GSTIN");
        // Leave other fields null

        String fileName = pdfGeneratorService.generateAndStorePdf(document);
        assertNotNull(fileName);

        Path filePath = Paths.get(PdfGeneratorService.PDF_STORAGE_PATH, fileName);
        assertTrue(Files.exists(filePath));
    }

    @Test
    public void testCachingWithFloatingPointValues() throws PdfGenerationException {
        Document document1 = mockData.createSampleDocument();
        Document document2 = mockData.createSampleDocument();

        // Modify the rate slightly but within double precision
        document2.getItems().get(0).setRate(100.000000001);

        String fileName1 = pdfGeneratorService.generateAndStorePdf(document1);
        String fileName2 = pdfGeneratorService.generateAndStorePdf(document2);

        assertEquals("Documents with negligible rate differences should be considered identical",
                fileName1, fileName2);
    }

    @Test
    public void testGeneratePdfWithLargeQuantity() throws PdfGenerationException {
        Document document = mockData.createSampleDocument();
        document.getItems().get(0).setQuantity("999999999");

        String fileName = pdfGeneratorService.generateAndStorePdf(document);
        assertNotNull(fileName);

        Path filePath = Paths.get(PdfGeneratorService.PDF_STORAGE_PATH, fileName);
        assertTrue(Files.exists(filePath));
    }

    @Test
    public void testGeneratePdfWithSpecialCharactersInQuantity() throws PdfGenerationException {
        Document document = mockData.createSampleDocument();
        document.getItems().get(0).setQuantity("10.5 KG");

        String fileName = pdfGeneratorService.generateAndStorePdf(document);
        assertNotNull(fileName);

        Path filePath = Paths.get(PdfGeneratorService.PDF_STORAGE_PATH, fileName);
        assertTrue(Files.exists(filePath));
    }

    @Test
    public void testValidDocument() {
        Document document = mockData.createSampleDocument();
        assertTrue("Document should be valid", pdfGeneratorService.isValidDocument(document));
    }

    @Test
    public void testDocumentWithNullGstin() {
        Document document = mockData.createSampleDocument();
        document.setSellerGstin(null);
        assertFalse("Document with null seller GSTIN should be invalid",
                pdfGeneratorService.isValidDocument(document));

        document = mockData.createSampleDocument();
        document.setBuyerGstin(null);
        assertFalse("Document with null buyer GSTIN should be invalid",
                pdfGeneratorService.isValidDocument(document));
    }

    @Test
    public void testDocumentWithOptionalFieldsMissing() {
        Document document = new Document();
        document.setSellerGstin("SELLER123");
        document.setBuyerGstin("BUYER456");
        document.setSeller(null);
        document.setBuyer(null);
        document.setSellerAddress(null);
        document.setBuyerAddress(null);

        List<Item> items = new ArrayList<>();
        Item item = new Item();
        item.setName("Test Item");
        item.setQuantity("1");
        items.add(item);
        document.setItems(items);

        assertTrue("Document with only required fields should be valid",
                pdfGeneratorService.isValidDocument(document));
    }
}