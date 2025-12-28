package org.example.reservation_event.services;

public interface PasswordResetService {
    boolean createPasswordResetRequest(String email);
    boolean resetPassword(String email, String code, String newPassword);
    boolean isValidResetCode(String email, String code);
}
