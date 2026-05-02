package com.jobtracker.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

/**
 * Service for extracting text content from uploaded files using Apache Tika.
 *
 * Supports: PDF, DOCX, DOC, and other formats Tika can parse.
 * Tika auto-detects file type and applies the appropriate parser.
 */
@Service
@Slf4j
public class TextExtractionService {

    private final Tika tika = new Tika();

    /**
     * Extract text content from an input stream.
     *
     * @param inputStream the file input stream
     * @param fileName    original filename (used for logging)
     * @return extracted text content, or empty string on failure
     */
    public String extractText(InputStream inputStream, String fileName) {
        try {
            String text = tika.parseToString(inputStream);
            log.info("Extracted {} characters from file: {}", text.length(), fileName);
            return text;
        } catch (IOException | TikaException e) {
            log.error("Failed to extract text from file: {}", fileName, e);
            return "";
        }
    }
}
