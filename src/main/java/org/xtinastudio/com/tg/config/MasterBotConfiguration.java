package org.xtinastudio.com.tg.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.xtinastudio.com.tg.bots.masterbot.service.MasterBot;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MasterBotConfiguration {

    private final MasterBot masterBot;

    @EventListener(ContextRefreshedEvent.class)
    public void init() {
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(masterBot);
        } catch (TelegramApiException e) {
            log.error("We cant create a bot: " + e.getMessage());
        }
    }
}
