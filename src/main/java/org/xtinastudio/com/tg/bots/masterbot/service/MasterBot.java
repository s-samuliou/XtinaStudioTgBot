package org.xtinastudio.com.tg.bots.masterbot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.xtinastudio.com.tg.properties.MasterBotProperties;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class MasterBot extends TelegramLongPollingBot {

    private final MasterBotProperties botProperties;

    private final MessageMasterService service;

    public MasterBot(MasterBotProperties botProperties, MessageMasterService service) {
        this.botProperties = botProperties;
        this.service = service;

        List<BotCommand> commandList = new ArrayList<>();
        commandList.add(new BotCommand("/start", "Начать использовать бот"));
        commandList.add(new BotCommand("/menu", "Главное меню"));
        commandList.add(new BotCommand("/booked_list", "Список забронированных услуг"));
        commandList.add(new BotCommand("/salon_location", "Показать местоположение салона"));
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
        BotApiMethod<?> message = service.messageReceiver(update);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Some error with answer:" + e.getMessage());
        }
    }
}
