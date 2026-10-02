package ru.artfly.bot.config;

import java.nio.file.Path;
import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Настройки студии ART FLY.
 *
 * @param dataDir       каталог с masters.csv и records.csv
 * @param zone          часовой пояс студии
 * @param telegramToken токен Telegram-бота (пустой — бот не запускается)
 */
@ConfigurationProperties("artfly")
public record ArtFlyProperties(Path dataDir, ZoneId zone, String telegramToken) {

    public ArtFlyProperties {
        if (dataDir == null) {
            dataDir = Path.of("data");
        }
        if (zone == null) {
            zone = ZoneId.systemDefault();
        }
    }

    public Path mastersFile() {
        return dataDir.resolve("masters.csv");
    }

    public Path recordsFile() {
        return dataDir.resolve("records.csv");
    }
}
