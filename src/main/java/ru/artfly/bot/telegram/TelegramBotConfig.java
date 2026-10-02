package ru.artfly.bot.telegram;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import ru.artfly.bot.assistant.PchelkaAssistant;
import ru.artfly.bot.config.ArtFlyProperties;

/** Запускает long polling Telegram, если задан artfly.telegram-token. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnExpression("!'${artfly.telegram-token:}'.isBlank()")
public class TelegramBotConfig {

    @Bean
    PchelkaTelegramBot pchelkaTelegramBot(ArtFlyProperties properties, PchelkaAssistant assistant) {
        return new PchelkaTelegramBot(new OkHttpTelegramClient(properties.telegramToken()), assistant);
    }

    @Bean
    TelegramBotsLongPollingApplication telegramBotsApplication(ArtFlyProperties properties, PchelkaTelegramBot bot)
            throws TelegramApiException {
        var application = new TelegramBotsLongPollingApplication();
        application.registerBot(properties.telegramToken(), bot);
        return application;
    }
}
