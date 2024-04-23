package org.xtinastudio.com.tg.bots.masterbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.xtinastudio.com.tg.properties.MasterBotProperties;

@Slf4j
@Component
@RequiredArgsConstructor
public class MasterBot extends TelegramLongPollingBot {

    private final MasterBotProperties botProperties;

    private final MessageMasterService service;

    @Override
    public String getBotUsername() {
        return botProperties.name();
    }

    @Override
    public String getBotToken() {
        return botProperties.token();
    }

    @Override
    public void onUpdateReceived(Update update) {
        SendMessage sendMessage = service.messageReceiver(update);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Some error with answer:" + e.getMessage());
        }
    }
}
