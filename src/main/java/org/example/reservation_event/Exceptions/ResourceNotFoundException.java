package org.example.reservation_event.Exceptions;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException() {
        super("La ressource demandée est introuvable.");
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}