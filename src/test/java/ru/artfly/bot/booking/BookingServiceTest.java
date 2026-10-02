package ru.artfly.bot.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ru.artfly.bot.config.ArtFlyProperties;

class BookingServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Moscow");
    private static final LocalDateTime SLOT = LocalDateTime.of(2026, 10, 5, 14, 0);

    @TempDir
    Path dataDir;

    private ArtFlyProperties properties;
    private BookingService service;

    @BeforeEach
    void setUp() throws IOException {
        properties = new ArtFlyProperties(dataDir, ZONE, "");
        Files.writeString(properties.mastersFile(), "Анна\nМария\n", StandardCharsets.UTF_8);
        Clock clock = Clock.fixed(Instant.parse("2026-10-02T09:00:00Z"), ZONE);
        service = new BookingService(new MasterRepository(properties), new BookingRepository(properties), clock);
    }

    @Test
    void writesBookingToCsv() throws IOException {
        service.book("Иван", SLOT, "анна");

        assertThat(Files.readAllLines(properties.recordsFile(), StandardCharsets.UTF_8))
                .containsExactly("Иван,2026-10-05 14:00,Анна");
    }

    @Test
    void rejectsSecondClientForSameMasterAndTime() {
        service.book("Иван", SLOT, "Анна");

        assertThatThrownBy(() -> service.book("Пётр", SLOT, "Анна"))
                .isInstanceOf(BookingException.class)
                .hasMessageContaining("уже занято");
        assertThat(service.busySlots("Анна", LocalDate.of(2026, 10, 5))).containsExactly(SLOT);
    }

    @Test
    void allowsSameTimeWithAnotherMaster() {
        service.book("Иван", SLOT, "Анна");
        service.book("Пётр", SLOT, "Мария");

        assertThat(new BookingRepository(properties).findAll()).hasSize(2);
    }

    @Test
    void rejectsUnknownMaster() {
        assertThatThrownBy(() -> service.book("Иван", SLOT, "Ольга"))
                .isInstanceOf(BookingException.class)
                .hasMessageContaining("Анна, Мария");
    }

    @Test
    void rejectsPastTime() {
        assertThatThrownBy(() -> service.book("Иван", LocalDateTime.of(2026, 10, 1, 10, 0), "Анна"))
                .isInstanceOf(BookingException.class)
                .hasMessageContaining("прошедшее");
    }

    @Test
    void quotesNamesWithCommas() {
        service.book("Иванов, Иван", SLOT, "Анна");

        assertThat(new BookingRepository(properties).findAll())
                .containsExactly(new Booking("Иванов, Иван", SLOT, "Анна"));
    }
}
