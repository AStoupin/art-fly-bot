package ru.artfly.bot.booking;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Repository;

import ru.artfly.bot.config.ArtFlyProperties;

/** Журнал записей в data/records.csv: имя клиента, дата и время записи, мастер. */
@Repository
public class BookingRepository {

    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ArtFlyProperties properties;

    public BookingRepository(ArtFlyProperties properties) {
        this.properties = properties;
    }

    public List<Booking> findAll() {
        var file = properties.recordsFile();
        if (!Files.exists(file)) {
            return List.of();
        }
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8).stream()
                    .map(line -> line.replace("﻿", ""))
                    .filter(line -> !line.isBlank())
                    .map(Csv::parseLine)
                    .filter(fields -> fields.size() >= 3)
                    .map(fields -> new Booking(fields.get(0), LocalDateTime.parse(fields.get(1), DATE_TIME), fields.get(2)))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось прочитать " + file, e);
        }
    }

    public void append(Booking booking) {
        var file = properties.recordsFile();
        String line = Csv.formatLine(booking.clientName(), booking.dateTime().format(DATE_TIME), booking.master());
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.writeString(file, line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось записать " + file, e);
        }
    }
}
