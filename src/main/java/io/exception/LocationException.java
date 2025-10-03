package io.exception;

public class LocationException extends AppException {
    public LocationException(String message) {
        super(message);
    }

    public LocationException(String message, Throwable cause) {
        super(message, cause);
    }
}
