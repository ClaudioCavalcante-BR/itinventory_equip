package br.com.infnet.itinventory.exception;

import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
public class ApiError {

    private final OffsetDateTime timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;

    public ApiError(
                    OffsetDateTime timestamp,
                    int status,
                    String error,
                    String message,
                    String path) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }
}
