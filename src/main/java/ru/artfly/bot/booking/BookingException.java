package ru.artfly.bot.booking;

/** Запись невозможна; сообщение понятно клиенту. */
public class BookingException extends RuntimeException {

    public BookingException(String message) {
        super(message);
    }
}
