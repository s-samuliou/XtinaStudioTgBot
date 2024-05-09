package org.xtinastudio.com.tg.bots.clientbot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.xtinastudio.com.tg.properties.ClientBotProperties;

import java.util.ArrayList;
import java.util.List;


@Slf4j
@Component
public class ClientBot extends TelegramLongPollingBot {

    private final ClientBotProperties botProperties;

    private final MessageClientService service;

    public ClientBot(ClientBotProperties botProperties, MessageClientService service) {
        this.botProperties = botProperties;
        this.service = service;

        List<BotCommand> commandList = new ArrayList<>();
        commandList.add(new BotCommand("/start", "Начать использовать бот"));
        commandList.add(new BotCommand("/menu", "Главное меню"));
        commandList.add(new BotCommand("/my_services", "Показать забронированные услуги"));
        commandList.add(new BotCommand("/about", "Информация о салоне"));
        commandList.add(new BotCommand("/masters", "Информация о мастерах"));
        commandList.add(new BotCommand("/reentry_phone_number", "Ввести новый номер телефона"));
        commandList.add(new BotCommand("/change_salon", "Поменять салон"));
        try {
            this.execute(new SetMyCommands(commandList, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Cant create menu: " + e.getMessage());
        }
    }

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
        BotApiMethod<?> message = service.mainCommands(update);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Some error with answer:" + e.getMessage());
        }
    }
}
