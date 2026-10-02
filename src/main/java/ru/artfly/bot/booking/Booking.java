package ru.artfly.bot.booking;

import java.time.LocalDateTime;

/** Запись клиента на стрижку: имя клиента, дата и время, мастер. */
public record Booking(String clientName, LocalDateTime dateTime, String master) {
}
