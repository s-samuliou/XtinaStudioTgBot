package org.xtinastudio.com.tg.bots.clientbot.service;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendLocation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.xtinastudio.com.entity.*;
import org.xtinastudio.com.enums.AppointmentStatus;
import org.xtinastudio.com.enums.Language;
import org.xtinastudio.com.enums.WorkTime;
import org.xtinastudio.com.service.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class MessageClientService {

    @Autowired
    private ClientService clientService;

    @Autowired
    private SalonInfoService salonInfo;

    @Autowired
    private ServiceService serviceService;

    @Autowired
    private MasterService masterService;

    @Autowired
    private AppointmentService appointmentService;

    private BookingState state = new BookingState();

    public BotApiMethod<?> mainCommands(Update update) {
        SendMessage sendMessage = new SendMessage();
        SendLocation sendLocation = new SendLocation();
        Long chatId = null;

        if (update.hasMessage() && update.getMessage().hasText()) {
            chatId = update.getMessage().getChatId();
            String text = update.getMessage().getText();

            switch (text) {
                case "/start":
                    sendMessage = registerClient(update);
                    break;
                case "/menu":
                    sendMessage = menu(chatId);
                    break;
                case "/about":
                    sendMessage = aboutSalon(chatId);
                    break;
                case "/masters":
                    sendMessage = aboutMasters(chatId);
                    break;
                case "/salon_location":
                    sendMessage = selectSalonLocation(chatId);
                    break;
                default:
                    sendMessage.setText("I dont know this message :(");
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
                    state = new BookingState();
                    return sendMessage;
                case "getToSalon":
                    String address = getDataCallbackQuery(data, 1);
                    sendLocation = sendSalonLocation(chatId, address);
                    return sendLocation;
                case "bookService":
                    sendMessage = bookService(chatId, state);
                    return sendMessage;
                case "cancelService":
                    sendMessage = cancelService(chatId);
                    return sendMessage;
                case "aboutSalon":
                    sendMessage = aboutSalon(chatId);
                    return sendMessage;
                case "ourMasters":
                    sendMessage = aboutMasters(chatId);
                    return sendMessage;
                case "wayToSalon":
                    sendMessage = selectSalonLocation(chatId);
                    return sendMessage;
                case "service":
                    String service = getDataCallbackQuery(data, 1);
                    Services serviceByName = serviceService.findByName(service);
                    state.setService(serviceByName);
                    sendMessage = bookService(chatId, state);
                    return sendMessage;
                case "master":
                    String master = getDataCallbackQuery(data, 1);
                    Master masterById = masterService.findById(Long.parseLong(master));
                    state.setMaster(masterById);
                    sendMessage = bookService(chatId, state);
                    return sendMessage;
                case "date":
                    String date = getDataCallbackQuery(data, 1);
                    LocalDate localDate = LocalDate.parse(date);
                    state.setDate(localDate);
                    sendMessage = bookService(chatId, state);
                    return sendMessage;
                case "time":
                    String time = getDataCallbackQuery(data, 1);
                    WorkTime workTime = parseWorkTime(time);
                    state.setWorkTime(workTime);
                    sendMessage = approveBookingService(chatId, state);
                    return sendMessage;
                case "approve":
                    Appointment appointment = new Appointment();
                    appointment.setService(state.getService());
                    appointment.setMaster(state.getMaster());
                    appointment.setAppointmentDate(state.getDate());
                    appointment.setAppointmentTime(state.getWorkTime());
                    appointment.setClient(clientService.findByChatId(chatId));
                    appointment.setStatus(AppointmentStatus.BANNED);

                    appointmentService.create(appointment);
                    state = new BookingState();
                    sendMessage = menu(chatId);
                    return sendMessage;
                default:
                    // Обработка непредвиденных нажатий
                    break;
            }
        }
        return null;
    }

    public WorkTime parseWorkTime(String time) {
        switch (time) {
            case "9:00":
                return WorkTime.NINE;
            case "10:00":
                return WorkTime.TEN;
            case "11:00":
                return WorkTime.ELEVEN;
            case "12:00":
                return WorkTime.TWELVE;
            default:
                throw new IllegalArgumentException("Invalid time format: " + time);
        }
    }

    public SendMessage approveBookingService(Long chatId, BookingState state) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);

        Services service = state.getService();
        Master master = state.getMaster();
        LocalDate date = state.getDate();
        WorkTime workTime = state.getWorkTime();

        StringBuilder text = new StringBuilder();
        text.append("Подтвердите выбор услуги:\n")
                .append(service.getName()).append("\n")
                .append(master.getName()).append("\n")
                .append(date.toString()).append("\n")
                .append(workTime.getDescription());

        sendMessage.setText(text.toString());

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        InlineKeyboardButton approveButton = new InlineKeyboardButton();
        approveButton.setText("Подтверждаю");
        approveButton.setCallbackData("approve");
        List<InlineKeyboardButton> approveButtonRow = new ArrayList<>();
        approveButtonRow.add(approveButton);
        keyboard.add(approveButtonRow);

        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("Back to Menu");
        backButton.setCallbackData("menu");
        List<InlineKeyboardButton> backButtonRow = new ArrayList<>();
        backButtonRow.add(backButton);
        keyboard.add(backButtonRow);

        markup.setKeyboard(keyboard);
        sendMessage.setReplyMarkup(markup);

        return sendMessage;
    }

    public SendMessage bookService(Long chatId, BookingState state) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);

        if (!state.checkService()) {
            sendMessage.setText("Выберите услугу:");
            List<Services> allServices = serviceService.getAll();

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

            for (Services service : allServices) {
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(service.getName());
                button.setCallbackData("service_" + service.getName());

                List<InlineKeyboardButton> row = new ArrayList<>();
                row.add(button);

                keyboard.add(row);
            }

            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("Back to Menu");
            button.setCallbackData("menu");
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            keyboard.add(row);

            markup.setKeyboard(keyboard);
            sendMessage.setReplyMarkup(markup);

            return sendMessage;
        }

        if (!state.checkMaster()) {
            sendMessage.setText("Выберите мастера:");
            List<Master> allMasters = masterService.findByServicesContaining(state.getService());

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

            for (Master master : allMasters) {
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(master.getName());
                button.setCallbackData("master_" + master.getId());

                List<InlineKeyboardButton> row = new ArrayList<>();
                row.add(button);

                keyboard.add(row);
            }

            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("Back to Menu");
            button.setCallbackData("menu");
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            keyboard.add(row);

            markup.setKeyboard(keyboard);
            sendMessage.setReplyMarkup(markup);

            return sendMessage;
        }

        if (!state.checkDate()) {
            sendMessage.setText("Выберите дату:");
            List<LocalDate> allDates = getAvailableDates();

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

            int buttonsInRow = 3;
            for (int i = 0; i < allDates.size(); i += buttonsInRow) {
                List<InlineKeyboardButton> row = new ArrayList<>();
                for (int j = i; j < Math.min(i + buttonsInRow, allDates.size()); j++) {
                    LocalDate date = allDates.get(j);
                    InlineKeyboardButton button = new InlineKeyboardButton();
                    button.setText(date.toString());
                    button.setCallbackData("date_" + date.toString());
                    row.add(button);
                }
                keyboard.add(row);
            }

            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("Back to Menu");
            button.setCallbackData("menu");
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            keyboard.add(row);

            markup.setKeyboard(keyboard);
            sendMessage.setReplyMarkup(markup);

            return sendMessage;
        }

        if (!state.checkTime()) {
            sendMessage.setText("Выберите время:");

            List<Appointment> appointments = appointmentService.getAppointmentsByDateAndServiceAndMaster(
                    state.getDate(), state.getMaster());

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

            int buttonsInRow = 3;
            List<InlineKeyboardButton> row = new ArrayList<>();
            for (WorkTime workTime : WorkTime.values()) {
                int workTimeValue = workTime.ordinal();

                boolean isTimeOccupied = appointments.stream()
                        .anyMatch(appointment -> appointment.getAppointmentTime().ordinal() == workTimeValue);

                if (!isTimeOccupied) {
                    InlineKeyboardButton button = new InlineKeyboardButton();
                    button.setText(workTime.getDescription());
                    button.setCallbackData("time_" + workTime.getDescription());

                    row.add(button);

                    if (row.size() == buttonsInRow) {
                        keyboard.add(row);
                        row = new ArrayList<>();
                    }
                }
            }

            // Если есть оставшиеся кнопки в последней строке, добавьте их
            if (!row.isEmpty()) {
                keyboard.add(row);
            }

            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("Back to Menu");
            button.setCallbackData("menu");
            List<InlineKeyboardButton> backButtonRow = new ArrayList<>();
            backButtonRow.add(button);
            keyboard.add(backButtonRow);

            markup.setKeyboard(keyboard);
            sendMessage.setReplyMarkup(markup);

            return sendMessage;
        }

        return null;
    }

    public List<LocalDate> getAvailableDates() {
        List<LocalDate> availableDates = new ArrayList<>();
        LocalDate currentDate = LocalDate.now();
        int daysToAdd = 1;

        while (availableDates.size() < 6) {
            LocalDate nextDate = currentDate.plusDays(daysToAdd);
            if (nextDate.getDayOfWeek() != DayOfWeek.SATURDAY && nextDate.getDayOfWeek() != DayOfWeek.SUNDAY) {
                availableDates.add(nextDate);
            }
            daysToAdd++;
        }

        return availableDates;
    }

    public SendMessage cancelService(Long chatId) {
        return null;
    }

    public SendMessage menu(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Выберите действие из меню:");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton button1 = new InlineKeyboardButton();
        button1.setText("Запись на услугу");
        button1.setCallbackData("bookService");
        row1.add(button1);
        keyboard.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton button2 = new InlineKeyboardButton();
        button2.setText("Отмена записи на услугу");
        button2.setCallbackData("cancelService");
        row2.add(button2);
        keyboard.add(row2);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton button3 = new InlineKeyboardButton();
        button3.setText("О салоне");
        button3.setCallbackData("aboutSalon");
        row3.add(button3);
        keyboard.add(row3);

        List<InlineKeyboardButton> row4 = new ArrayList<>();
        InlineKeyboardButton button4 = new InlineKeyboardButton();
        button4.setText("Наши мастера");
        button4.setCallbackData("ourMasters");
        row4.add(button4);
        keyboard.add(row4);

        List<InlineKeyboardButton> row5 = new ArrayList<>();
        InlineKeyboardButton button5 = new InlineKeyboardButton();
        button5.setText("Добраться до салона");
        button5.setCallbackData("wayToSalon");
        row5.add(button5);
        keyboard.add(row5);

        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);

        return message;
    }

    public SendMessage selectSalonLocation(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Select salon location:");

        List<SalonInfo> allSalons = salonInfo.getAll();

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (SalonInfo salon : allSalons) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(salon.getName());
            button.setCallbackData("getToSalon_" + salon.getName());

            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);

            keyboard.add(row);
        }

        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText("Back to Menu");
        button.setCallbackData("menu");
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(button);
        keyboard.add(row);

        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);

        return message;
    }

    public SendLocation sendSalonLocation(Long chatId, String address) {
        SendLocation sendLocation = new SendLocation();

        SalonInfo salon = salonInfo.findByName(address);

        if (salon != null) {
            sendLocation.setChatId(chatId);
            sendLocation.setLatitude(salon.getLatitude());
            sendLocation.setLongitude(salon.getLongitude());
        }

        return sendLocation;
    }

    private static String getDataCallbackQuery(String data, int x) {
        String[] parts = data.split("_");
        String address = parts[x];
        return address;
    }

    public SendMessage aboutMasters(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("It's information about our masters!");

        return sendMessage;
    }

    public SendMessage aboutSalon(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("It's information about this salon!");

        return sendMessage;
    }

    public SendMessage registerClient(Update update) {
        SendMessage sendMessage = new SendMessage();
        if (!clientService.existsByChatId(update.getMessage().getChatId())) {
            Client client = new Client();
            client.setName(update.getMessage().getChat().getFirstName());
            client.setChatId(update.getMessage().getChatId());
            client.setLanguage(Language.RUSSIAN);
            client.setRegistrationDate(LocalDate.now());

            if (update.getMessage().getContact() != null && update.getMessage().getContact().getPhoneNumber() != null) {
                client.setPhoneNumber(update.getMessage().getContact().getPhoneNumber());
            }

            clientService.create(client);
            Hibernate.initialize(client);
            sendMessage.setText(String.format("Welcome %s! You are automatically registered and can work with the bot!", update.getMessage().getChat().getFirstName()));
            sendMessage.setChatId(update.getMessage().getChatId());
        } else {
            sendMessage.setText("You are already registered and can work with the bot!");
            sendMessage.setChatId(update.getMessage().getChatId());
        }

        return sendMessage;
    }
}
