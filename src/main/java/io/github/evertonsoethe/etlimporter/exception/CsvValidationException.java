package io.github.evertonsoethe.etlimporter.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CsvValidationException extends RuntimeException {

    private final HttpStatus status;
    private final String userMessage;

    public CsvValidationException(HttpStatus status, String userMessage) {
        super(userMessage);
        this.status = status;
        this.userMessage = userMessage;
    }
}
