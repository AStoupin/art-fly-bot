package ru.artfly.bot.assistant;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import ru.artfly.bot.booking.BookingException;
import ru.artfly.bot.booking.BookingRepository;
import ru.artfly.bot.booking.BookingService;

/**
 * Инструменты, через которые Пчелка работает с записями. Ошибки возвращаются текстом,
 * чтобы модель могла объяснить их клиенту и предложить другой вариант.
 */
@Component
public class BookingTools {

    private static final Logger log = LoggerFactory.getLogger(BookingTools.class);

    private final BookingService bookingService;

    public BookingTools(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Tool(description = "Список мастеров студии ART FLY, к которым можно записаться")
    public List<String> listMasters() {
        List<String> masters = bookingService.masters();
        log.info("listMasters() -> {}", masters);
        return masters;
    }

    @Tool(description = "Занятое время мастера на указанную дату (список в формате yyyy-MM-dd HH:mm)")
    public String busySlots(
            @ToolParam(description = "Имя мастера из списка мастеров") String master,
            @ToolParam(description = "Дата в формате yyyy-MM-dd") String date) {
        String result = findBusySlots(master, date);
        log.info("busySlots({}, {}) -> {}", master, date, result);
        return result;
    }

    @Tool(description = "Записать клиента на стрижку. Вызывай только после того, как клиент подтвердил имя, дату, время и мастера")
    public String createBooking(
            @ToolParam(description = "Имя клиента") String clientName,
            @ToolParam(description = "Дата и время записи в формате yyyy-MM-dd HH:mm") String dateTime,
            @ToolParam(description = "Имя мастера из списка мастеров") String master) {
        String result = book(clientName, dateTime, master);
        log.info("createBooking({}, {}, {}) -> {}", clientName, dateTime, master, result);
        return result;
    }

    private String findBusySlots(String master, String date) {
        try {
            List<String> busy = bookingService.busySlots(master, LocalDate.parse(date)).stream()
                    .map(dt -> dt.format(BookingRepository.DATE_TIME))
                    .toList();
            return busy.isEmpty() ? "На эту дату у мастера записей нет." : "Занято: " + String.join(", ", busy);
        } catch (DateTimeParseException e) {
            return "Ошибка: дата должна быть в формате yyyy-MM-dd.";
        } catch (BookingException e) {
            return "Ошибка: " + e.getMessage();
        }
    }

    private String book(String clientName, String dateTime, String master) {
        try {
            var booking = bookingService.book(clientName, LocalDateTime.parse(dateTime, BookingRepository.DATE_TIME), master);
            return "Запись создана: " + booking.clientName() + ", "
                    + booking.dateTime().format(BookingRepository.DATE_TIME) + ", мастер " + booking.master() + ".";
        } catch (DateTimeParseException e) {
            return "Ошибка: дата и время должны быть в формате yyyy-MM-dd HH:mm.";
        } catch (BookingException e) {
            return "Ошибка: " + e.getMessage();
        }
    }
}
