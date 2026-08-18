package io.github.evertonsoethe.etlimporter.service;

import io.github.evertonsoethe.etlimporter.config.ImportProperties;
import io.github.evertonsoethe.etlimporter.exception.CsvValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CsvValidationService {

    private static final List<String> EXPECTED_COLUMNS = List.of(
            "sale_id", "sale_date", "customer_id", "customer_name",
            "product_id", "product_name", "category", "quantity",
            "unit_price", "total_price", "payment_method", "status"
    );

    private final ImportProperties importProperties;

    public void validate(MultipartFile file) {
        validatePresence(file);
        validateExtension(file.getOriginalFilename());
        validateNotEmpty(file);
        validateSize(file);
        validateHeader(file);
    }

    private void validatePresence(MultipartFile file) {
        if (file == null) {
            throw new CsvValidationException(HttpStatus.BAD_REQUEST,
                    "Multipart file is required.");
        }
    }

    private void validateExtension(String filename) {
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new CsvValidationException(HttpStatus.BAD_REQUEST,
                    "Only .csv files are accepted.");
        }
    }

    private void validateNotEmpty(MultipartFile file) {
        if (file.isEmpty() || file.getSize() == 0) {
            throw new CsvValidationException(HttpStatus.BAD_REQUEST,
                    "The file is empty.");
        }
    }

    private void validateSize(MultipartFile file) {
        DataSize maxSize = DataSize.parse(importProperties.getMaxFileSize());
        if (file.getSize() > maxSize.toBytes()) {
            throw new CsvValidationException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "File exceeds the size limit of " + importProperties.getMaxFileSize() + ".");
        }
    }

    private void validateHeader(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream()))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw new CsvValidationException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "The file does not contain a header.");
            }

            List<String> actualColumns = Arrays.stream(headerLine.split(","))
                    .map(String::trim)
                    .toList();

            if (actualColumns.size() != EXPECTED_COLUMNS.size()) {
                throw new CsvValidationException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Expected columns: " + EXPECTED_COLUMNS + ", found: " + actualColumns);
            }

            // Check if columns are correct but in wrong order
            if (actualColumns.containsAll(EXPECTED_COLUMNS) && !actualColumns.equals(EXPECTED_COLUMNS)) {
                throw new CsvValidationException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Column order is incorrect. Expected: " + EXPECTED_COLUMNS);
            }

            // Check if columns are different
            if (!actualColumns.equals(EXPECTED_COLUMNS)) {
                throw new CsvValidationException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Expected columns: " + EXPECTED_COLUMNS + ", found: " + actualColumns);
            }

        } catch (IOException e) {
            throw new CsvValidationException(HttpStatus.BAD_REQUEST,
                    "Error reading the file header.");
        }
    }
}
