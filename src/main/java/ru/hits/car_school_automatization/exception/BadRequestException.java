package ru.hits.car_school_automatization.exception;

/**
 * Исключение (HTTP 400)
 */
public class BadRequestException extends RuntimeException {
    
    public BadRequestException(String message) {
        super(message);
    }
}
