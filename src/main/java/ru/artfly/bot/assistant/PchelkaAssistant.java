package ru.artfly.bot.assistant;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/** Пчелка — менеджер ART FLY по записи на стрижки. Хранит историю диалога отдельно для каждого клиента. */
@Service
public class PchelkaAssistant {

    private static final Logger log = LoggerFactory.getLogger(PchelkaAssistant.class);

    private static final DateTimeFormatter NOW_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String CONTINUE_REQUEST =
            "[служебное] Твой предыдущий ответ клиенту был пустым. Напиши клиенту ответ по итогам своих действий.";

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final Resource systemPrompt;
    private final Clock clock;

    public PchelkaAssistant(ChatClient.Builder builder, ChatMemory chatMemory, BookingTools tools, Clock clock,
            @Value("classpath:prompts/pchelka-system.st") Resource systemPrompt) {
        this.chatClient = builder
                .defaultTools(tools)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
        this.chatMemory = chatMemory;
        this.systemPrompt = systemPrompt;
        this.clock = clock;
    }

    public String reply(String conversationId, String userMessage) {
        String answer = ask(conversationId, userMessage);
        if (answer == null || answer.isBlank()) {
            // После вызова инструментов модель иногда завершает ход без текста — просим ответить клиенту явно.
            log.warn("Пустой ответ модели в диалоге {}, повторный запрос", conversationId);
            answer = ask(conversationId, CONTINUE_REQUEST);
        }
        return answer == null ? "" : answer.strip();
    }

    private String ask(String conversationId, String userMessage) {
        LocalDateTime now = LocalDateTime.now(clock);
        return chatClient.prompt()
                .system(s -> s.text(systemPrompt)
                        .param("now", now.format(NOW_FORMAT))
                        .param("weekday", now.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("ru"))))
                .user(userMessage)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }

    /** Начать диалог заново (команда /start). */
    public void reset(String conversationId) {
        chatMemory.clear(conversationId);
    }
}
