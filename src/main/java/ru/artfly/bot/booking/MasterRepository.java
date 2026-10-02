package ru.artfly.bot.booking;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import ru.artfly.bot.config.ArtFlyProperties;

/** Справочник мастеров из data/masters.csv (одно имя мастера на строку). */
@Repository
public class MasterRepository {

    private final ArtFlyProperties properties;

    public MasterRepository(ArtFlyProperties properties) {
        this.properties = properties;
    }

    /** Файл читается при каждом вызове, чтобы правки списка мастеров подхватывались без перезапуска. */
    public List<String> findAll() {
        var file = properties.mastersFile();
        if (!Files.exists(file)) {
            return List.of();
        }
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8).stream()
                    .map(line -> line.replace("﻿", ""))
                    .filter(line -> !line.isBlank())
                    .map(line -> Csv.parseLine(line).getFirst())
                    .filter(name -> !name.isBlank())
                    .distinct()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось прочитать " + file, e);
        }
    }

    /** Ищет мастера без учёта регистра и возвращает имя в написании из справочника. */
    public Optional<String> findByName(String name) {
        String needle = name.trim().toLowerCase(Locale.ROOT);
        return findAll().stream()
                .filter(master -> master.toLowerCase(Locale.ROOT).equals(needle))
                .findFirst();
    }
}
