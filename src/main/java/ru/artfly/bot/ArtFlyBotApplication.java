package ru.artfly.bot;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

import ru.artfly.bot.config.ArtFlyProperties;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ArtFlyBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArtFlyBotApplication.class, args);
    }

    @Bean
    Clock clock(ArtFlyProperties properties) {
        return Clock.system(properties.zone());
    }
}
