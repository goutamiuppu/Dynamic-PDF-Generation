package org.assignment.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assignment.domainmodel.Document;
import org.assignment.mockdata.MockDocumentData;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@RunWith(SpringRunner.class)
public class PdfControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    private MockDocumentData mockData;

    @Before
    public void setUp() {
        objectMapper = new ObjectMapper();
        mockData = new MockDocumentData();
    }

    @Test
    public void testGenerateAndDownloadPdf() throws Exception {
        Document document = mockData.createSampleDocument();
        String documentJson = objectMapper.writeValueAsString(document);

        MvcResult result = mockMvc.perform(post("/pdf/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(documentJson))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andReturn();

        String fileName = result.getResponse().getContentAsString();
        assertNotNull("Generated filename should not be null", fileName);
        assertTrue("Filename should contain GSTIN", fileName.contains(document.getSellerGstin()));

        mockMvc.perform(get("/pdf/download/" + fileName))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF));
    }

    @Test
    public void testGeneratePdfWithInvalidDocument() throws Exception {
        // Test null document
        Document invalidDocument = new Document();
        String documentJson = objectMapper.writeValueAsString(invalidDocument);

        mockMvc.perform(post("/pdf/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(documentJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("Invalid document: Required fields are missing"));
    }

    @Test
    public void testGeneratePdfWithMissingGstin() throws Exception {
        Document document = mockData.createSampleDocument();
        document.setSellerGstin(null);
        String documentJson = objectMapper.writeValueAsString(document);

        mockMvc.perform(post("/pdf/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(documentJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN));
    }

    @Test
    public void testGeneratePdfWithEmptyItems() throws Exception {
        Document document = mockData.createSampleDocument();
        document.setItems(new ArrayList<>());
        String documentJson = objectMapper.writeValueAsString(document);

        mockMvc.perform(post("/pdf/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(documentJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN));
    }

    @Test
    public void testDownloadNonexistentPdf() throws Exception {
        mockMvc.perform(get("/pdf/download/nonexistent.pdf"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGeneratePdfWithSpecialCharacters() throws Exception {
        Document document = mockData.createSampleDocument();
        document.getItems().get(0).setQuantity("10.5 KG");
        document.setBuyerAddress("Line 1\nLine 2"); // Test newline characters

        String documentJson = objectMapper.writeValueAsString(document);

        MvcResult result = mockMvc.perform(post("/pdf/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(documentJson))
                .andExpect(status().isOk())
                .andReturn();

        String fileName = result.getResponse().getContentAsString();

        mockMvc.perform(get("/pdf/download/" + fileName))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    public void testCachingBehavior() throws Exception {
        // Generate first PDF
        Document document = mockData.createSampleDocument();
        String documentJson = objectMapper.writeValueAsString(document);

        MvcResult firstResult = mockMvc.perform(post("/pdf/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(documentJson))
                .andExpect(status().isOk())
                .andReturn();

        String firstFileName = firstResult.getResponse().getContentAsString();

        // Generate second PDF with same content
        MvcResult secondResult = mockMvc.perform(post("/pdf/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(documentJson))
                .andExpect(status().isOk())
                .andReturn();

        String secondFileName = secondResult.getResponse().getContentAsString();

        assertEquals("Same content should return same filename", firstFileName, secondFileName);
    }

    @Test
    public void testGeneratePdfWithLargeDocument() throws Exception {
        Document document = mockData.createLargeDocument();
        String documentJson = objectMapper.writeValueAsString(document);

        MvcResult result = mockMvc.perform(post("/pdf/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(documentJson))
                .andExpect(status().isOk())
                .andReturn();

        String fileName = result.getResponse().getContentAsString();

        mockMvc.perform(get("/pdf/download/" + fileName))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

}