package ru.artfly.bot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"spring.ai.anthropic.api-key=test-key", "artfly.telegram-token="})
class ArtFlyBotApplicationTests {

    @Test
    void contextLoads() {
    }
}
