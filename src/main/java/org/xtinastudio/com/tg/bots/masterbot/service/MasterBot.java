package org.xtinastudio.com.tg.bots.masterbot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendLocation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.xtinastudio.com.entity.*;
import org.xtinastudio.com.enums.AppointmentStatus;
import org.xtinastudio.com.service.AppointmentService;
import org.xtinastudio.com.service.MasterService;
import org.xtinastudio.com.service.SalonService;
import org.xtinastudio.com.tg.properties.MasterBotProperties;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class MasterBot extends TelegramLongPollingBot {

    @Autowired
    private MasterService masterService;

    @Autowired
    private SalonService salonService;

    @Autowired
    private AppointmentService appointmentService;

    private final MasterBotProperties botProperties;

    public MasterBot(MasterService masterService, SalonService salonService, AppointmentService appointmentService, MasterBotProperties botProperties) {
        this.botProperties = botProperties;
        this.masterService = masterService;
        this.salonService = salonService;
        this.appointmentService = appointmentService;

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
        BotApiMethod<?> message = messageReceiver(update);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Some error with answer:" + e.getMessage());
        }
    }

    public BotApiMethod<?> messageReceiver(Update update) {
        SendMessage sendMessage = new SendMessage();
        SendLocation sendLocation = new SendLocation();
        Long chatId = null;

        if (update.hasMessage() && update.getMessage().hasText()) {
            chatId = update.getMessage().getChatId();
            String text = update.getMessage().getText();

            switch (text) {
                case "/start":
                    sendMessage = start(chatId);
                    break;
                case "/menu":
                    sendMessage = menu(chatId);
                    break;
                case "/myServices":
                    sendMessage = showBookedServices(chatId);
                    break;
                case "/salon_location":
                    sendLocation = sendSalonLocation(chatId);
                    return sendLocation;
                default:
                    if (checkLoginAndPassword(text)) {
                        String login = splitLoginAndPassword(text, 0);
                        String password = splitLoginAndPassword(text, 1);

                        sendMessage = authorizeMaster(chatId, login, password);
                    } else {
                        sendMessage.setText("Я не знаю такой команды :(\nВызовите главное меню через menu.");
                    }
                    break;
            }

            return sendMessage;
        } else if (update.hasCallbackQuery()) {
            chatId = update.getCallbackQuery().getMessage().getChatId();
            String data = update.getCallbackQuery().getData();
            String prefix = getDataCallbackQuery(data, 0);

            switch (prefix) {
                case "menu":
                    sendMessage = menu(chatId);
                    return sendMessage;
                case "myServices":
                    sendMessage = showBookedServices(chatId);
                    return sendMessage;
                case "wayToSalon":
                    sendLocation = sendSalonLocation(chatId);
                    return sendLocation;
                case "reauthorize":
                    sendMessage = start(chatId);
                    return sendMessage;
                default:
                    break;
            }
        }

        return sendMessage;
    }

    private SendMessage start(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        StringBuilder text = new StringBuilder();

        if (masterService.existsByChatId(chatId)) {
            text.append("Вы уже авторизованы! Можете пользоваться ботом!");

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

            addMainMenuButton(keyboard);

            markup.setKeyboard(keyboard);
            sendMessage.setReplyMarkup(markup);
        } else {
            text.append("Добро пожаловать в чат-бот для мастеров!\n")
                    .append("Введите пожалуйста ваши логин и пароль через пробел (login password)");
        }

        sendMessage.setText(text.toString());

        return sendMessage;
    }

    private SendMessage showBookedServices(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        StringBuilder text = new StringBuilder();
        text.append("Забронированные услуги:\n");

        Master master = masterService.findByChatId(chatId);
        List<Appointment> appointmentsByMaster = appointmentService.getAppointmentsByMaster(master);

        for (Appointment appointment : appointmentsByMaster) {
            if (appointment.getStatus().equals(AppointmentStatus.BANNED)) {
                text.append("Клиент: ").append(appointment.getClient().getName()).append("\n")
                        .append("Услуга: ").append(appointment.getService().getName()).append("\n")
                        .append("Дата: ").append(appointment.getAppointmentDate()).append("\n")
                        .append("Время: ").append(appointment.getAppointmentTime()).append("\n\n");
            }
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        addMainMenuButton(keyboard);

        markup.setKeyboard(keyboard);
        sendMessage.setReplyMarkup(markup);

        sendMessage.setText(text.toString());
        return sendMessage;
    }


    private SendMessage authorizeMaster(Long chatId, String login, String password) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);

        Master master = masterService.findByLogin(login);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        if (master != null) {
            if (master.getPassword().equals(password)) {
                sendMessage.setText("Вы успешно вошли в систему!");
                master.setChatId(chatId);
                masterService.editById(master.getId(), master);
            } else {
                sendMessage.setText("Пароль введён неверно!");

                List<InlineKeyboardButton> row2 = new ArrayList<>();
                InlineKeyboardButton button2 = new InlineKeyboardButton();
                button2.setText("Ввести данные заново");
                button2.setCallbackData("reauthorize");
                row2.add(button2);
                keyboard.add(row2);
            }
        } else {
            sendMessage.setText("Данного логина не существует!");

            List<InlineKeyboardButton> row2 = new ArrayList<>();
            InlineKeyboardButton button2 = new InlineKeyboardButton();
            button2.setText("Ввести данные заново");
            button2.setCallbackData("reauthorize");
            row2.add(button2);
            keyboard.add(row2);
        }

        markup.setKeyboard(keyboard);
        sendMessage.setReplyMarkup(markup);

        return sendMessage;
    }

    private SendMessage menu(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Выберите действие из меню:");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton button1 = new InlineKeyboardButton();
        button1.setText("Посмотреть записи");
        button1.setCallbackData("myServices");
        row1.add(button1);
        keyboard.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton button2 = new InlineKeyboardButton();
        button2.setText("Добраться до салона");
        button2.setCallbackData("wayToSalon");
        row2.add(button2);
        keyboard.add(row2);


        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);

        return message;
    }

    public SendLocation sendSalonLocation(Long chatId) {
        SendLocation sendLocation = new SendLocation();

        Master master = masterService.findByChatId(chatId);
        Salon salon = salonService.findByName(master.getSalon().getAddress());

        if (salon != null) {
            sendLocation.setChatId(chatId);
            sendLocation.setLatitude(salon.getLatitude());
            sendLocation.setLongitude(salon.getLongitude());

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

            addMainMenuButton(keyboard);

            markup.setKeyboard(keyboard);
            sendLocation.setReplyMarkup(markup);
        }

        return sendLocation;
    }

    public SendMessage sendBookedNoticeToMaster(Appointment appointment) {
        SendMessage sendMessage = new SendMessage();
        StringBuilder text = new StringBuilder();

        Master master = appointment.getMaster();
        Client client = appointment.getClient();
        Services service = appointment.getService();

        sendMessage.setChatId(master.getChatId());

        text.append("У вас новая запись!\n")
                .append("Клиент: ").append(client.getName())
                .append("Услуга: ").append(service.getName())
                .append("Дата: ").append(appointment.getAppointmentDate())
                .append("Время: ").append(appointment.getAppointmentTime());

        sendMessage.setText(text.toString());
        log.info("Master chatId - " + master.getChatId());
        log.info("Client name - " + client.getName());
        log.info("Service name - " + service.getName());

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Some error with answer:" + e.getMessage());
        }

        return sendMessage;
    }

    public SendMessage sendCanceledNoticeToMaster(Appointment appointment) {
        SendMessage sendMessage = new SendMessage();
        StringBuilder text = new StringBuilder();

        Master master = appointment.getMaster();
        Client client = appointment.getClient();
        Services service = appointment.getService();

        sendMessage.setChatId(master.getChatId());

        text.append("У вас отмена записи!\n")
                .append("Клиент: ").append(client.getName())
                .append("Услуга: ").append(service.getName())
                .append("Дата: ").append(appointment.getAppointmentDate())
                .append("Время: ").append(appointment.getAppointmentTime());

        sendMessage.setText(text.toString());

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Some error with answer:" + e.getMessage());
        }

        return sendMessage;
    }

    private void addMainMenuButton(List<List<InlineKeyboardButton>> keyboard) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText("Главное меню");
        button.setCallbackData("menu");
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(button);
        keyboard.add(row);
    }

    private String getDataCallbackQuery(String data, int x) {
        String[] parts = data.split("_");
        String address = parts[x];
        return address;
    }

    private String splitLoginAndPassword(String data, int x) {
        String[] parts = data.split(" ");
        String address = parts[x];
        return address;
    }

    private boolean checkLoginAndPassword(String data) {
        String[] parts = data.split(" ");
        return parts.length == 2;
    }
}
