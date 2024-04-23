package org.xtinastudio.com.tg.bots.masterbot.service;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Service
public class MessageMasterService {
    public SendMessage messageReceiver(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();
            String firstName = update.getMessage().getChat().getFirstName();

            String response;

            switch (text) {
                case "/start" -> response = String.format("Hello, %s!", firstName);
                case "/stop" -> response = String.format("Bye, %s!", firstName);
                default -> response = String.format("I dont know this message :(");
            }

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(response);
            return sendMessage;
        }

        return null;
    }
}
