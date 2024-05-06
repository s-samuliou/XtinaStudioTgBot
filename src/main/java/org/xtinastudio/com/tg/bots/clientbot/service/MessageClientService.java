package org.xtinastudio.com.tg.bots.clientbot.service;

import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
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
import org.xtinastudio.com.tg.bots.masterbot.service.MasterNotice;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class MessageClientService {

    @Autowired
    private ClientService clientService;

    @Autowired
    private SalonService salonService;

    @Autowired
    private ServiceService serviceService;

    @Autowired
    private MasterService masterService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private MasterNotice masterNotice;

    private BookingState state = new BookingState();

    private Long canceledAppointment = null;

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
                    sendLocation = sendSalonLocation(chatId);
                    return sendLocation;
                case "/change_salon":
                    sendMessage = selectSalon(chatId);
                    break;
                default:
                    sendMessage.setText("Я не знаю такой команды :(\nВызовите главное меню через menu.");
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
                case "wayToSalon":
                    sendLocation = sendSalonLocation(chatId);
                    return sendLocation;
                case "bookService":
                    sendMessage = bookService(chatId, state);
                    return sendMessage;
                case "aboutSalon":
                    sendMessage = aboutSalon(chatId);
                    return sendMessage;
                case "ourMasters":
                    sendMessage = aboutMasters(chatId);
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
                    masterNotice.sendBookedNoticeToMaster(appointment);
                    return sendMessage;
                case "myServices":
                    sendMessage = myServices(chatId);
                    return sendMessage;
                case "cancelService":
                    sendMessage = cancelService(chatId);
                    return sendMessage;
                case "approveCancel":
                    String idFormMessage = getDataCallbackQuery(data, 1);
                    Long id = Long.parseLong(idFormMessage);
                    Appointment appointmentById = appointmentService.getById(id);
                    appointmentById.setStatus(AppointmentStatus.CANCELED);
                    appointmentService.editById(id, appointmentById);
                    masterNotice.sendCanceledNoticeToMaster(appointmentById);
                    sendMessage = menu(chatId);
                    return sendMessage;
                case "chooseCancel":
                    String message = getDataCallbackQuery(data, 1);
                    Long idMessage = Long.parseLong(message);
                    sendMessage = approveCancel(chatId, idMessage);
                    return sendMessage;
                case "selectSalon":
                    String salonId = getDataCallbackQuery(data, 1);
                    Client client = clientService.findByChatId(chatId);
                    client.setSalon(salonService.findById(Long.parseLong(salonId)));
                    clientService.editById(client.getId(), client);
                    sendMessage = menu(chatId);
                    return sendMessage;
                default:
                    // Обработка непредвиденных нажатий
                    break;
            }
        }
        return null;
    }

    public SendMessage selectSalon(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        StringBuilder text = new StringBuilder();

        List<Salon> salons = salonService.getAll();

        if (clientService.findByChatId(chatId).getSalon() == null) {
            text.append(":sparkles:").append("Поздравляем! Вы автоматически зарегестрированы в этом боте!\n");
        }

        text.append(":point_down:").append("Выберите салон для бронирования услуг").append(":point_down:");
        sendMessage.setText(convertToEmoji(text.toString()));
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (Salon salon : salons) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(salon.getAddress());
            button.setCallbackData("selectSalon_" + salon.getId());
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            keyboard.add(row);
        }

        markup.setKeyboard(keyboard);
        sendMessage.setReplyMarkup(markup);

        return sendMessage;
    }

    public SendMessage myServices(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        Client client = clientService.findByChatId(chatId);
        List<Appointment> appointmentsByClient = appointmentService.getAppointmentsByClient(client);

        StringBuilder messageText = new StringBuilder(":mag_right: Ваши забронированные услуги :mag_right:\n");
        for (Appointment appointment : appointmentsByClient) {
            if (appointment.getStatus() == AppointmentStatus.BANNED) {
                messageText
                        .append(":bell: ").append("Услуга: " + appointment.getService().getName()).append("\n")
                        .append(":woman_artist: ").append("Мастер: " + appointment.getMaster().getName()).append("\n")
                        .append(":calendar: ").append("Дата: " + appointment.getAppointmentDate()).append("\n")
                        .append(":mantelpiece_clock: ").append("Время: " + appointment.getAppointmentTime().getDescription()).append("\n")
                        .append(":money_with_wings: ").append("Цена: " + appointment.getService().getPrice()).append("\n\n");
            }
        }

        sendMessage.setText(convertToEmoji(messageText.toString()));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        addMainMenuButton(keyboard);

        markup.setKeyboard(keyboard);
        sendMessage.setReplyMarkup(markup);

        return sendMessage;
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
        text.append(":point_down: Подтвердите выбор услуги :point_down:\n")
                .append(":bell: ").append(service.getName()).append("\n")
                .append(":woman_artist: ").append(master.getName()).append("\n")
                .append(":calendar: ").append(date.toString()).append("\n")
                .append(":mantelpiece_clock: ").append(workTime.getDescription());

        sendMessage.setText(convertToEmoji(text.toString()));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        InlineKeyboardButton approveButton = new InlineKeyboardButton();
        approveButton.setText(convertToEmoji(":white_check_mark: Подтверждаю :white_check_mark:"));
        approveButton.setCallbackData("approve");
        List<InlineKeyboardButton> approveButtonRow = new ArrayList<>();
        approveButtonRow.add(approveButton);
        keyboard.add(approveButtonRow);

        addMainMenuButton(keyboard);

        markup.setKeyboard(keyboard);
        sendMessage.setReplyMarkup(markup);;

        return sendMessage;
    }

    public SendMessage bookService(Long chatId, BookingState state) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);

        Client client = clientService.findByChatId(chatId);
        Salon clientSalon = client.getSalon();

        if (!state.checkService()) {
            sendMessage.setText(convertToEmoji(":point_down: Выберите услугу :point_down:\n"));
            List<Services> allServices = serviceService.findBySalons(clientSalon);

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

            for (Services service : allServices) {
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(convertToEmoji(":fleur_de_lis:" + service.getName() + ":fleur_de_lis:"));
                button.setCallbackData("service_" + service.getName());

                List<InlineKeyboardButton> row = new ArrayList<>();
                row.add(button);

                keyboard.add(row);
            }

            addMainMenuButton(keyboard);

            markup.setKeyboard(keyboard);
            sendMessage.setReplyMarkup(markup);

            return sendMessage;
        }

        if (!state.checkMaster()) {
            sendMessage.setText(convertToEmoji(":woman_artist: Выберите мастера :point_down:\n"));
            List<Master> allMasters = masterService.findByServicesContainingAndSalon(state.getService(), clientSalon);

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

            for (Master master : allMasters) {
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(convertToEmoji(":star:" + master.getName() + ":star:"));
                button.setCallbackData("master_" + master.getId());

                List<InlineKeyboardButton> row = new ArrayList<>();
                row.add(button);

                keyboard.add(row);
            }

            addMainMenuButton(keyboard);

            markup.setKeyboard(keyboard);
            sendMessage.setReplyMarkup(markup);

            return sendMessage;
        }

        if (!state.checkDate()) {
            sendMessage.setText(convertToEmoji(":calendar: Выберите дату :point_down:\n"));
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

            addMainMenuButton(keyboard);

            markup.setKeyboard(keyboard);
            sendMessage.setReplyMarkup(markup);

            return sendMessage;
        }

        if (!state.checkTime()) {
            sendMessage.setText(convertToEmoji(":mantelpiece_clock: Выберите время :point_down:\n"));

            List<Appointment> appointments = appointmentService.getAppointmentsByDateAndServiceAndMaster(
                    state.getDate(), state.getMaster());

            LocalDateTime currentDateTime = LocalDateTime.now();
            int currentHour = currentDateTime.getHour();
            int currentMinute = currentDateTime.getMinute();

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

            int buttonsInRow = 3;
            List<InlineKeyboardButton> row = new ArrayList<>();
            for (WorkTime workTime : WorkTime.values()) {
                int workTimeValue = workTime.ordinal();

                if (!(state.getDate().isEqual(LocalDate.now()) && workTimeValue <= currentHour - 9) && isTimeInPast(workTime, currentHour, currentMinute)) {
                    boolean isTimeOccupied = appointments.stream()
                            .anyMatch(appointment -> appointment.getAppointmentTime().ordinal() == workTimeValue
                                    && appointment.getStatus() != AppointmentStatus.CANCELED);

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
            }

            if (!row.isEmpty()) {
                keyboard.add(row);
            }

            addMainMenuButton(keyboard);

            markup.setKeyboard(keyboard);
            sendMessage.setReplyMarkup(markup);

            return sendMessage;
        }

        return null;
    }

    private boolean isTimeInPast(WorkTime workTime, int currentHour, int currentMinute) {
        int timeHour = workTime.ordinal() / 2 + 10;
        int timeMinute = workTime.ordinal() % 2 == 0 ? 0 : 30;

        return (currentHour > timeHour || (currentHour == timeHour && currentMinute >= timeMinute + 15));
    }

    public List<LocalDate> getAvailableDates() {
        List<LocalDate> availableDates = new ArrayList<>();
        LocalDate currentDate = LocalDate.now();
        int daysToAdd = 1;

        availableDates.add(currentDate);

        while (availableDates.size() < 11) {
            LocalDate nextDate = currentDate.plusDays(daysToAdd);
            if (nextDate.getDayOfWeek() != DayOfWeek.SATURDAY && nextDate.getDayOfWeek() != DayOfWeek.SUNDAY) {
                availableDates.add(nextDate);
            }
            daysToAdd++;
        }

        return availableDates;
    }

    public SendMessage approveCancel(Long chatId, Long appointmentId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(convertToEmoji(":question_mark: Вы действительно хотите отменить услугу :question_mark:\n"));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        InlineKeyboardButton yesButton = new InlineKeyboardButton();
        yesButton.setText(convertToEmoji(":white_check_mark: Да :white_check_mark:"));
        yesButton.setCallbackData("approveCancel_" + appointmentId);
        List<InlineKeyboardButton> yesButtonRow = new ArrayList<>();
        yesButtonRow.add(yesButton);
        keyboard.add(yesButtonRow);

        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(convertToEmoji(":x: Нет :x:"));
        backButton.setCallbackData("menu");
        List<InlineKeyboardButton> backButtonRow = new ArrayList<>();
        backButtonRow.add(backButton);
        keyboard.add(backButtonRow);

        markup.setKeyboard(keyboard);
        sendMessage.setReplyMarkup(markup);

        return sendMessage;
    }

    public SendMessage cancelService(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        int counter = 1;

        Client client = clientService.findByChatId(chatId);
        List<Appointment> appointmentsByClient = appointmentService.getAppointmentsByClient(client);

        StringBuilder messageText = new StringBuilder(convertToEmoji(":spiral_notepad: Ваши забронированные услуги :point_down:\n"));
        for (Appointment appointment : appointmentsByClient) {
            if (appointment.getStatus() == AppointmentStatus.BANNED) {
                messageText
                        .append("Номер: ").append(counter++).append("\n")
                        .append(":bell: Услуга: " + appointment.getService().getName()).append("\n")
                        .append(":woman_artist: Мастер: " + appointment.getMaster().getName()).append("\n")
                        .append(":calendar: Дата: " + appointment.getAppointmentDate()).append("\n")
                        .append(":mantelpiece_clock:: Время: " + appointment.getAppointmentTime().getDescription()).append("\n")
                        .append(":money_with_wings: Цена: " + appointment.getService().getPrice()).append("\n\n");
            }
        }

        sendMessage.setText(convertToEmoji(messageText.toString()));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        counter = 1;

        for (Appointment appointment : appointmentsByClient) {
            if (appointment.getStatus() == AppointmentStatus.BANNED) {
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(convertToEmoji(":x: Отменить " + counter + " :x:"));
                button.setCallbackData("chooseCancel_" + appointment.getId());
                List<InlineKeyboardButton> row = new ArrayList<>();
                row.add(button);
                keyboard.add(row);
                counter++;
            }
        }

        addMainMenuButton(keyboard);

        markup.setKeyboard(keyboard);
        sendMessage.setReplyMarkup(markup);

        return sendMessage;
    }

    public SendMessage menu(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(convertToEmoji(":point_down: Выберите действие из меню :point_down:"));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton button1 = new InlineKeyboardButton();
        button1.setText(convertToEmoji(":calendar: Запись на услугу :calendar:"));
        button1.setCallbackData("bookService");
        row1.add(button1);
        keyboard.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton button2 = new InlineKeyboardButton();
        button2.setText(convertToEmoji(":x: Отмена записи :x:"));
        button2.setCallbackData("cancelService");
        row2.add(button2);
        keyboard.add(row2);

        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(convertToEmoji(":mag_right: Мои записи :mag_right:"));
        button.setCallbackData("myServices");
        row.add(button);
        keyboard.add(row);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton button3 = new InlineKeyboardButton();
        button3.setText(convertToEmoji(":office: О салоне :office:"));
        button3.setCallbackData("aboutSalon");
        row3.add(button3);
        keyboard.add(row3);

        List<InlineKeyboardButton> row4 = new ArrayList<>();
        InlineKeyboardButton button4 = new InlineKeyboardButton();
        button4.setText(convertToEmoji(":woman_artist: Наши мастера :woman_artist:"));
        button4.setCallbackData("ourMasters");
        row4.add(button4);
        keyboard.add(row4);

        List<InlineKeyboardButton> row5 = new ArrayList<>();
        InlineKeyboardButton button5 = new InlineKeyboardButton();
        button5.setText(convertToEmoji(":oncoming_taxi: Добраться до салона :oncoming_taxi:"));
        button5.setCallbackData("wayToSalon");
        row5.add(button5);
        keyboard.add(row5);

        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);

        return message;
    }

    public SendLocation sendSalonLocation(Long chatId) {
        SendLocation sendLocation = new SendLocation();

        Client client = clientService.findByChatId(chatId);
        Salon salon = salonService.findByName(client.getSalon().getAddress());

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

    private static String getDataCallbackQuery(String data, int x) {
        String[] parts = data.split("_");
        String address = parts[x];
        return address;
    }

    public SendMessage aboutMasters(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("Здесь будет информация о мастрах!");

        return sendMessage;
    }

    public SendMessage aboutSalon(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("Здесь будет информация о этом салоне!");

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
            sendMessage = selectSalon(update.getMessage().getChatId());
        } else {
            sendMessage.setText(convertToEmoji(":fireworks: Вы уже зарегестрированы и можете пользоваться ботом! :fireworks:"));
            sendMessage.setChatId(update.getMessage().getChatId());

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

            addMainMenuButton(keyboard);

            markup.setKeyboard(keyboard);
            sendMessage.setReplyMarkup(markup);
        }

        return sendMessage;
    }

    private void addMainMenuButton(List<List<InlineKeyboardButton>> keyboard) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(convertToEmoji(":house_with_garden: Главное меню :house_with_garden:"));
        button.setCallbackData("menu");
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(button);
        keyboard.add(row);
    }

    private String convertToEmoji(String text) {
        String s = EmojiParser.parseToUnicode(text);
        return s;
    }
}
