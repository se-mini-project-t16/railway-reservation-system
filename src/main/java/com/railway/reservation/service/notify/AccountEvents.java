package com.railway.reservation.service.notify;

/** Account emails sent after the change is committed. The code lives only in memory until it is emailed. */
public final class AccountEvents {

    private AccountEvents() {}

    public record PasswordResetRequested(String email, String name, String code, int minutes) {}

    public record PasswordChanged(String email, String name) {}
}
