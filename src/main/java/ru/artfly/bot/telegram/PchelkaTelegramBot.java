package ru.artfly.bot.telegram;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import ru.artfly.bot.assistant.PchelkaAssistant;

/** Принимает сообщения из Telegram и передаёт их Пчелке. Каждый чат — отдельный диалог. */
public class PchelkaTelegramBot implements LongPollingSingleThreadUpdateConsumer, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(PchelkaTelegramBot.class);

    private static final String GREETING_REQUEST = "Здравствуйте! Хочу записаться на стрижку.";

    private static final String FALLBACK_REPLY = "Извините, я задумалась. Повторите, пожалуйста, ваше последнее сообщение.";

    private final TelegramClient telegramClient;
    private final PchelkaAssistant assistant;
    /** Ответ модели занимает секунды, поэтому чаты обслуживаются параллельно, не блокируя опрос Telegram. */
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public PchelkaTelegramBot(TelegramClient telegramClient, PchelkaAssistant assistant) {
        this.telegramClient = telegramClient;
        this.assistant = assistant;
    }

    @Override
    public void consume(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }
        long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText().trim();
        executor.submit(() -> handle(chatId, text));
    }

    private void handle(long chatId, String text) {
        String conversationId = "telegram-" + chatId;
        try {
            String userMessage = text;
            if (text.startsWith("/start")) {
                assistant.reset(conversationId);
                userMessage = GREETING_REQUEST;
            }
            telegramClient.execute(SendChatAction.builder().chatId(chatId).action(ActionType.TYPING.toString()).build());
            String reply = assistant.reply(conversationId, userMessage);
            send(chatId, reply.isBlank() ? FALLBACK_REPLY : reply);
        } catch (Exception e) {
            log.error("Ошибка обработки сообщения в чате {}", chatId, e);
            send(chatId, "Ой, у меня что-то пошло не так. Попробуйте, пожалуйста, ещё раз чуть позже.");
        }
    }

    private void send(long chatId, String text) {
        try {
            telegramClient.execute(SendMessage.builder().chatId(chatId).text(text).build());
        } catch (TelegramApiException e) {
            log.error("Не удалось отправить сообщение в чат {}", chatId, e);
        }
    }

    @Override
    public void close() {
        executor.close();
    }
}
