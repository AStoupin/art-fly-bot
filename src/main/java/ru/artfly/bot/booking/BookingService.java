package ru.artfly.bot.booking;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;

/** Правила записи на стрижку. */
@Service
public class BookingService {

    private final MasterRepository masters;
    private final BookingRepository bookings;
    private final Clock clock;

    public BookingService(MasterRepository masters, BookingRepository bookings, Clock clock) {
        this.masters = masters;
        this.bookings = bookings;
        this.clock = clock;
    }

    public List<String> masters() {
        return masters.findAll();
    }

    /** Занятое время мастера на указанную дату. */
    public List<LocalDateTime> busySlots(String master, LocalDate date) {
        String canonical = requireMaster(master);
        return bookings.findAll().stream()
                .filter(b -> b.master().equalsIgnoreCase(canonical))
                .map(Booking::dateTime)
                .filter(dt -> dt.toLocalDate().equals(date))
                .sorted()
                .toList();
    }

    public boolean isFree(String master, LocalDateTime dateTime) {
        String canonical = requireMaster(master);
        LocalDateTime slot = dateTime.truncatedTo(ChronoUnit.MINUTES);
        return bookings.findAll().stream()
                .noneMatch(b -> b.master().equalsIgnoreCase(canonical) && b.dateTime().equals(slot));
    }

    /**
     * Записывает клиента. Проверка и запись в файл выполняются под одной блокировкой,
     * чтобы к мастеру на одно время не попало больше одного клиента.
     */
    public synchronized Booking book(String clientName, LocalDateTime dateTime, String master) {
        if (clientName == null || clientName.isBlank()) {
            throw new BookingException("Не указано имя клиента.");
        }
        String canonical = requireMaster(master);
        LocalDateTime slot = dateTime.truncatedTo(ChronoUnit.MINUTES);
        if (!slot.isAfter(LocalDateTime.now(clock))) {
            throw new BookingException("Нельзя записаться на прошедшее время: "
                    + slot.format(BookingRepository.DATE_TIME) + ".");
        }
        if (!isFree(canonical, slot)) {
            throw new BookingException("У мастера " + canonical + " время "
                    + slot.format(BookingRepository.DATE_TIME) + " уже занято.");
        }
        var booking = new Booking(clientName.trim(), slot, canonical);
        bookings.append(booking);
        return booking;
    }

    private String requireMaster(String master) {
        if (master == null || master.isBlank()) {
            throw new BookingException("Не указан мастер.");
        }
        return masters.findByName(master)
                .orElseThrow(() -> new BookingException("Мастера «" + master.trim()
                        + "» нет в студии. Доступные мастера: " + String.join(", ", masters.findAll()) + "."));
    }
}
