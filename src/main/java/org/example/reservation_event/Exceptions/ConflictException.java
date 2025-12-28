package org.example.reservation_event.Exceptions;

public class ConflictException extends RuntimeException {

    public ConflictException() {
        super("Conflit de données.");
    }

    public ConflictException(String message) {
        super(message);
    }
}