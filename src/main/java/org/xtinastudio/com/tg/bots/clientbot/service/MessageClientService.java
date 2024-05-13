package org.xtinastudio.com.tg.bots.clientbot.service;

import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendLocation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private MasterReviewService masterReviewService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private MasterNotice masterNotice;

    private BookingState state = new BookingState();

    private Long canceledAppointment = null;

    public BotApiMethod<?> mainCommands(Update update) {
        EditMessageText editMessage = new EditMessageText();
        SendMessage sendMessage = new SendMessage();
        SendLocation sendLocation = new SendLocation();
        Long chatId = null;
        Long messageId = null;

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
                case "/instruction":
                    sendMessage = instruction(chatId);
                    break;
                case "/reentry_phone_number":
                    sendMessage = inputPhoneNumberMessage(chatId);
                    break;
                case "/change_salon":
                    sendMessage = selectSalon(chatId);
                    break;
                default:
                    if (isValidPhoneNumber(text)) {
                        sendMessage = inputPhoneNumber(chatId, text);
                    } else {
                        sendMessage.setChatId(chatId);
                        sendMessage.setText("Я не знаю такой команды :(\nВызовите главное меню через menu.");
                    }
                    break;
            }

            return sendMessage;
        } else if (update.hasCallbackQuery()) {
            chatId = update.getCallbackQuery().getMessage().getChatId();
            messageId = update.getCallbackQuery().getMessage().getMessageId().longValue();

            String data = update.getCallbackQuery().getData();
            String prefix = getDataCallbackQuery(data, 0);

            switch (prefix) {
                case "menuSendMessage":
                    sendMessage = menu(chatId);
                    state = new BookingState();
                    return sendMessage;
                case "menu":
                    editMessage = menu(chatId, messageId);
                    state = new BookingState();
                    return editMessage;
                case "next":
                    editMessage = selectSalon(chatId, messageId);
                    state = new BookingState();
                    return editMessage;
                case "wayToSalon":
                    sendLocation = sendSalonLocation(chatId);
                    return sendLocation;
                case "bookService":
                    editMessage = bookService(chatId, state, messageId);
                    return editMessage;
                case "aboutSalon":
                    editMessage = aboutSalon(chatId, messageId);
                    return editMessage;
                case "ourMasters":
                    editMessage = aboutMasters(chatId, messageId);
                    return editMessage;
                case "service":
                    String service = getDataCallbackQuery(data, 1);
                    Services serviceByName = serviceService.findByName(service);
                    state.setService(serviceByName);
                    editMessage = bookService(chatId, state, messageId);
                    return editMessage;
                case "master":
                    String master = getDataCallbackQuery(data, 1);
                    Master masterById = masterService.findById(Long.parseLong(master));
                    state.setMaster(masterById);
                    editMessage = bookService(chatId, state, messageId);
                    return editMessage;
                case "date":
                    String date = getDataCallbackQuery(data, 1);
                    LocalDate localDate = LocalDate.parse(date);
                    state.setDate(localDate);
                    editMessage = bookService(chatId, state, messageId);
                    return editMessage;
                case "time":
                    String time = getDataCallbackQuery(data, 1);
                    WorkTime workTime = parseWorkTime(time);
                    state.setWorkTime(workTime);
                    editMessage = approveBookingService(chatId, state, messageId);
                    return editMessage;
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
                    editMessage = menu(chatId, messageId);
                    if (appointment.getMaster().getChatId() != null) {
                        masterNotice.sendBookedNoticeToMaster(appointment);
                    }
                    return editMessage;
                case "myServices":
                    editMessage = myServices(chatId, messageId);
                    return editMessage;
                case "cancelService":
                    editMessage = cancelService(chatId, messageId);
                    return editMessage;
                case "approveCancel":
                    String idFormMessage = getDataCallbackQuery(data, 1);
                    Long id = Long.parseLong(idFormMessage);
                    Appointment appointmentById = appointmentService.getById(id);
                    appointmentById.setStatus(AppointmentStatus.CANCELED);
                    appointmentService.editById(id, appointmentById);
                    if (appointmentById.getMaster().getChatId() != null) {
                        masterNotice.sendCanceledNoticeToMaster(appointmentById);
                    }
                    editMessage = menu(chatId, messageId);
                    return editMessage;
                case "chooseCancel":
                    String message = getDataCallbackQuery(data, 1);
                    Long idMessage = Long.parseLong(message);
                    editMessage = approveCancel(chatId, idMessage, messageId);
                    return editMessage;
                case "selectSalon":
                    String salonId = getDataCallbackQuery(data, 1);
                    Client client = clientService.findByChatId(chatId);
                    client.setSalon(salonService.findById(Long.parseLong(salonId)));
                    clientService.editById(client.getId(), client);
                    editMessage = menu(chatId, messageId);
                    return editMessage;
                case "selectSalonRegistration":
                    String salonReg = getDataCallbackQuery(data, 1);
                    Client clientReg = clientService.findByChatId(chatId);
                    clientReg.setSalon(salonService.findById(Long.parseLong(salonReg)));
                    clientService.editById(clientReg.getId(), clientReg);
                    editMessage = congratulationRegistration(chatId, messageId);
                    return editMessage;
                case "navigation":
                    editMessage = navigation(chatId, messageId);
                    return editMessage;
                case "instruction":
                    editMessage = instruction(chatId, messageId);
                    return editMessage;
                case "backToServices":
                    state.setService(null);
                    editMessage = bookService(chatId, state, messageId);
                    return editMessage;
                case "backToMasters":
                    state.setMaster(null);
                    editMessage = bookService(chatId, state, messageId);
                    return editMessage;
                case "backToDate":
                    state.setDate(null);
                    editMessage = bookService(chatId, state, messageId);
                    return editMessage;
                case "backToTime":
                    state.setWorkTime(null);
                    editMessage = bookService(chatId, state, messageId);
                    return editMessage;
                default:
                    break;
            }
        }
        return null;
    }

    private SendMessage instruction(Long chatId) {
        if (!isAuthorized(chatId)) {
            return sendNonAuthorizedMessage(chatId);
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        StringBuilder text = new StringBuilder();

        text.append(":scroll: Инструкция\n\n")
                .append(":one: Обратите внимание, что слева внизу от клавиатуры есть встроенное 'menu' c помощью которого " +
                        "Вы можете выполнять различные команды напрямую.").append("\n\n")
                .append(":two: Команды встроенного меню:").append("\n")
                .append(" :two::one: Команда /start предназначена для регистрации.").append("\n")
                .append(" :two::two: Команда /меню вызвает главное меню, в котором можно делать/отменять записи и тд.").append("\n")
                .append(" :two::three: Команда / выдаёт инструкцию, как пользоваться этим ботом").append("\n")
                .append(" :two::four: Команда / позволяет ввести новый номер телефона, чтобы использовать его вместо старого").append("\n")
                .append(" :two::five: Команда / позволяет поменять салон для бронирования услуг").append("\n\n")
                .append(":three: Команды главного меню:").append("\n")
                .append(" :three::one: С помощью 'Запись на услугу' Вы можете записаться на услугу").append("\n")
                .append(" :three::two: С помощью 'Мои записи' Вы можете просмотреть действующие записи и в случае чего отменить их.").append("\n")
                .append(" :three::three: В разделе 'Навигация' Вы можете найти информацию о салоне, мастерах и инструкцию").append("\n")
                .append(" :three::four: С помощью 'Добраться до Салона' Вы можете нажав на карту - проложить путь до салона через навигатор").append("\n");

        sendMessage.setText(convertToEmoji(text.toString()));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        addMainMenuButton(keyboard);

        markup.setKeyboard(keyboard);
        sendMessage.setReplyMarkup(markup);

        return sendMessage;
    }

    private EditMessageText instruction(Long chatId, Long messageId) {
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(chatId);
        editMessageText.setMessageId(messageId.intValue());
        StringBuilder text = new StringBuilder();

        text.append(":scroll: Инструкция\n\n")
                .append(":one: Обратите внимание, что слева внизу от клавиатуры есть встроенное 'menu' c помощью которого " +
                        "Вы можете выполнять различные команды напрямую.").append("\n\n")
                .append(":two: Команды встроенного меню:").append("\n")
                .append(" :two::one: Команда /start предназначена для регистрации.").append("\n")
                .append(" :two::two: Команда /menu вызвает главное меню, в котором можно делать/отменять записи и тд.").append("\n")
                .append(" :two::three: Команда /instruction выдаёт инструкцию, как пользоваться этим ботом").append("\n")
                .append(" :two::four: Команда /reentry_phone_number позволяет ввести новый номер телефона, чтобы использовать его вместо старого").append("\n")
                .append(" :two::five: Команда /change_salon позволяет поменять салон для бронирования услуг").append("\n\n")
                .append(":three: Команды главного меню:").append("\n")
                .append(" :three::one: С помощью 'Запись на услугу' Вы можете записаться на услугу").append("\n")
                .append(" :three::two: С помощью 'Мои записи' Вы можете просмотреть действующие записи и в случае чего отменить их.").append("\n")
                .append(" :three::three: В разделе 'Навигация' Вы можете найти информацию о салоне, мастерах и инструкцию").append("\n")
                .append(" :three::four: С помощью 'Добраться до Салона' Вы можете нажав на карту - проложить путь до салона через навигатор").append("\n");

        editMessageText.setText(convertToEmoji(text.toString()));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        addMainMenuButton(keyboard);

        markup.setKeyboard(keyboard);
        editMessageText.setReplyMarkup(markup);

        return editMessageText;
    }

    public EditMessageText aboutMasters(Long chatId, Long messageId) {
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(chatId);
        StringBuilder text = new StringBuilder();

        if (messageId != null) {
            editMessageText.setMessageId(messageId.intValue());
        }

        Client client = clientService.findByChatId(chatId);
        Salon salon = salonService.findById(client.getSalon().getId());
        List<Master> masters = masterService.getAllBySalon(salon);

        for (Master master : masters) {
            text.append(":woman_artist: ").append("Мастер: ").append(master.getName()).append("\n")
                    .append(":star: ").append("Рейтинг: ").append(masterReviewService.getRatingByMaster(master)).append("\n")
                    .append(":link: ").append("Страничка: ").append(master.getUrl()).append("\n")
                    .append(":memo: ").append("Описание: ").append(master.getName()).append("\n");
        }

        editMessageText.setText(convertToEmoji(text.toString()));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        addMainMenuButton(keyboard);

        markup.setKeyboard(keyboard);
        editMessageText.setReplyMarkup(markup);

        return editMessageText;
    }

    public EditMessageText aboutSalon(Long chatId, Long messageId) {
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(chatId);

        if (messageId != null) {
            editMessageText.setMessageId(messageId.intValue());
        }

        Client client = clientService.findByChatId(chatId);
        Salon salon = salonService.findById(client.getSalon().getId());

        editMessageText.setText(convertToEmoji(salon.getDescription()));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        addMainMenuButton(keyboard);

        markup.setKeyboard(keyboard);
        editMessageText.setReplyMarkup(markup);

        return editMessageText;
    }

    private EditMessageText navigation(Long chatId, Long messageId) {
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(chatId);
        editMessageText.setMessageId(messageId.intValue());
        editMessageText.setText(convertToEmoji(":scroll: Это раздел с различной информацией связанной с салоном:"));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton button3 = new InlineKeyboardButton();
        button3.setText(convertToEmoji(":office: О салоне"));
        button3.setCallbackData("aboutSalon");
        row3.add(button3);
        keyboard.add(row3);

        List<InlineKeyboardButton> row4 = new ArrayList<>();
        InlineKeyboardButton button4 = new InlineKeyboardButton();
        button4.setText(convertToEmoji(":woman_artist: Наши мастера"));
        button4.setCallbackData("ourMasters");
        row4.add(button4);
        keyboard.add(row4);

        List<InlineKeyboardButton> row5 = new ArrayList<>();
        InlineKeyboardButton button5 = new InlineKeyboardButton();
        button5.setText(convertToEmoji(":memo: Инструкция"));
        button5.setCallbackData("instruction");
        row5.add(button5);
        keyboard.add(row5);

        addMainMenuButton(keyboard);

        markup.setKeyboard(keyboard);
        editMessageText.setReplyMarkup(markup);

        return editMessageText;
    }

    public EditMessageText congratulationRegistration(Long chatId, Long messageId) {
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(chatId);
        editMessageText.setMessageId(messageId.intValue());

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        editMessageText.setText(convertToEmoji(":tada: Поздравляем с успешной регистрацией! :tada: \n" +
                "Теперь вы часть нашего сообщества. Добро пожаловать!\n" +
                ":point_down: Чтобы продолжить, воспользуйтесь главным меню :point_down:"));

        addMainMenuButton(keyboard);

        markup.setKeyboard(keyboard);
        editMessageText.setReplyMarkup(markup);

        return editMessageText;
    }

    public SendMessage inputPhoneNumberMessage(Long chatId) {
        if (!isAuthorized(chatId)) {
            return sendNonAuthorizedMessage(chatId);
        }

        SendMessage editMessageText = new SendMessage();
        editMessageText.setChatId(chatId);

        Client client = clientService.findByChatId(chatId);

        if (client.getPhoneNumber() == null) {
            editMessageText.setText(convertToEmoji(":star: Добро пожаловать в чат-бот салона красоты xtina.studio! :star:\n" +
                    "Сейчас Вам будет нужно пройти небольшую регистрацию, после чего вы сможете полльзоваться ботом!\n" +
                    ":telephone: Для начала введите свой номер телефона.\n" +
                    "Без пробелов и тире с помощью клавиатуры (+123456789)\n"));
        } else {
            editMessageText.setText(convertToEmoji(":telephone: Введите новый номер телефона.\n" +
                    "Без пробелов и тире с помощью клавиатуры (+123456789)\n"));
        }

        return editMessageText;
    }

    public SendMessage inputPhoneNumber(Long chatId, String phoneNumber) {
        SendMessage editMessageText = new SendMessage();
        editMessageText.setChatId(chatId);

        Client client = clientService.findByChatId(chatId);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        if (client.getPhoneNumber() == null) {
            client.setPhoneNumber(phoneNumber);
            clientService.editById(client.getId(), client);
            editMessageText.setText(convertToEmoji(":star: Отлично! :star:\n" +
                    ":telephone: Ваш номер телефона сохранён!"));

            addNextButton(keyboard);
        } else {
            client.setPhoneNumber(phoneNumber);
            clientService.editById(client.getId(), client);
            editMessageText.setText(convertToEmoji(":star: Ваш номер успешно изменён! :star:\n" +
                    "Можете продолжить пользоваться ботом!"));

            addMainMenuButton(keyboard);
        }

        markup.setKeyboard(keyboard);
        editMessageText.setReplyMarkup(markup);

        return editMessageText;
    }

    public EditMessageText selectSalon(Long chatId, Long messageId) {
        EditMessageText sendMessage = new EditMessageText();
        sendMessage.setChatId(chatId);
        sendMessage.setMessageId(messageId.intValue());
        StringBuilder text = new StringBuilder();

        List<Salon> salons = salonService.getAll();

        text.append(":point_down:").append("Выберите салон для бронирования услуг").append(":point_down:");
        sendMessage.setText(convertToEmoji(text.toString()));
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (Salon salon : salons) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(convertToEmoji(":office:" + salon.getAddress()));
            button.setCallbackData("selectSalonRegistration_" + salon.getId());
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            keyboard.add(row);
        }

        markup.setKeyboard(keyboard);
        sendMessage.setReplyMarkup(markup);

        return sendMessage;
    }

    public SendMessage selectSalon(Long chatId) {
        if (!isAuthorized(chatId)) {
            return sendNonAuthorizedMessage(chatId);
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        StringBuilder text = new StringBuilder();

        List<Salon> salons = salonService.getAll();

        text.append(":point_down:").append("Теперь Вам нужно выбрать салон для бронирования услуг").append(":point_down:");
        sendMessage.setText(convertToEmoji(text.toString()));
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (Salon salon : salons) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(convertToEmoji(":office:" + salon.getAddress()));
            button.setCallbackData("selectSalon_" + salon.getId());
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            keyboard.add(row);
        }

        markup.setKeyboard(keyboard);
        sendMessage.setReplyMarkup(markup);

        return sendMessage;
    }

    public EditMessageText myServices(Long chatId, Long messageId) {
        EditMessageText sendMessage = new EditMessageText();
        sendMessage.setChatId(chatId);

        if (messageId != null) {
            sendMessage.setMessageId(messageId.intValue());
        }

        Client client = clientService.findByChatId(chatId);
        List<Appointment> appointmentsByClient = appointmentService.getAppointmentsByClient(client);

        StringBuilder messageText = new StringBuilder(":mag_right: Ваши забронированные услуги \n\n");
        for (Appointment appointment : appointmentsByClient) {
            if (appointment.getStatus() == AppointmentStatus.BANNED) {
                messageText
                        .append(":bell: ").append("Услуга: " + appointment.getService().getName()).append("\n")
                        .append(":woman_artist: ").append("Мастер: " + appointment.getMaster().getName()).append("\n")
                        .append(":calendar: ").append("Дата: " + appointment.getAppointmentDate()).append("\n")
                        .append(":mantelpiece_clock: ").append("Время: " + appointment.getAppointmentTime().getDescription()).append("\n")
                        .append(":money_with_wings: ").append("Цена: " + appointment.getService().getPrice()).append(" nis\n\n");
            }
        }

        sendMessage.setText(convertToEmoji(messageText.toString()));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton button2 = new InlineKeyboardButton();
        button2.setText(convertToEmoji(":x: Отмена записи"));
        button2.setCallbackData("cancelService");
        row2.add(button2);
        keyboard.add(row2);

        addMainMenuButton(keyboard);

        markup.setKeyboard(keyboard);
        sendMessage.setReplyMarkup(markup);

        return sendMessage;
    }

    public WorkTime parseWorkTime(String time) {
        switch (time) {
            case "8:00":
                return WorkTime.EIGHT;
            case "8:15":
                return WorkTime.EIGHT_FIFTEEN;
            case "8:30":
                return WorkTime.EIGHT_THIRTY;
            case "8:45":
                return WorkTime.EIGHT_FORTY_FIVE;
            case "9:00":
                return WorkTime.NINE;
            case "9:15":
                return WorkTime.NINE_FIFTEEN;
            case "9:30":
                return WorkTime.NINE_THIRTY;
            case "9:45":
                return WorkTime.NINE_FORTY_FIVE;
            case "10:00":
                return WorkTime.TEN;
            case "10:15":
                return WorkTime.TEN_FIFTEEN;
            case "10:30":
                return WorkTime.TEN_THIRTY;
            case "10:45":
                return WorkTime.TEN_FORTY_FIVE;
            case "11:00":
                return WorkTime.ELEVEN;
            case "11:15":
                return WorkTime.ELEVEN_FIFTEEN;
            case "11:30":
                return WorkTime.ELEVEN_THIRTY;
            case "11:45":
                return WorkTime.ELEVEN_FORTY_FIVE;
            case "12:00":
                return WorkTime.TWELVE;
            case "12:15":
                return WorkTime.TWELVE_FIFTEEN;
            case "12:30":
                return WorkTime.TWELVE_THIRTY;
            case "12:45":
                return WorkTime.TWELVE_FORTY_FIVE;
            case "13:00":
                return WorkTime.THIRTEEN;
            case "13:15":
                return WorkTime.THIRTEEN_FIFTEEN;
            case "13:30":
                return WorkTime.THIRTEEN_THIRTY;
            case "13:45":
                return WorkTime.THIRTEEN_FORTY_FIVE;
            case "14:00":
                return WorkTime.FOURTEEN;
            case "14:15":
                return WorkTime.FOURTEEN_FIFTEEN;
            case "14:30":
                return WorkTime.FOURTEEN_THIRTY;
            case "14:45":
                return WorkTime.FOURTEEN_FORTY_FIVE;
            case "15:00":
                return WorkTime.FIFTEEN;
            case "15:15":
                return WorkTime.FIFTEEN_FIFTEEN;
            case "15:30":
                return WorkTime.FIFTEEN_THIRTY;
            case "15:45":
                return WorkTime.FIFTEEN_FORTY_FIVE;
            case "16:00":
                return WorkTime.SIXTEEN;
            case "16:15":
                return WorkTime.SIXTEEN_FIFTEEN;
            case "16:30":
                return WorkTime.SIXTEEN_THIRTY;
            case "16:45":
                return WorkTime.SIXTEEN_FORTY_FIVE;
            case "17:00":
                return WorkTime.SEVENTEEN;
            case "17:15":
                return WorkTime.SEVENTEEN_FIFTEEN;
            case "17:30":
                return WorkTime.SEVENTEEN_THIRTY;
            case "17:45":
                return WorkTime.SEVENTEEN_FORTY_FIVE;
            case "18:00":
                return WorkTime.EIGHTEEN;
            case "18:15":
                return WorkTime.EIGHTEEN_FIFTEEN;
            case "18:30":
                return WorkTime.EIGHTEEN_THIRTY;
            case "18:45":
                return WorkTime.EIGHTEEN_FORTY_FIVE;
            case "19:00":
                return WorkTime.NINETEEN;
            default:
                throw new IllegalArgumentException("Invalid time format: " + time);
        }
    }

    public EditMessageText approveBookingService(Long chatId, BookingState state, Long messageId) {
        EditMessageText sendMessage = new EditMessageText();
        sendMessage.setChatId(chatId);

        if (messageId != null) {
            sendMessage.setMessageId(messageId.intValue());
        }

        Services service = state.getService();
        Master master = state.getMaster();
        LocalDate date = state.getDate();
        WorkTime workTime = state.getWorkTime();

        StringBuilder text = new StringBuilder();
        text.append(":point_down: Подтвердите выбор услуги :point_down:\n\n")
                .append(":bell: ").append("Услуга: ").append(service.getName()).append("\n")
                .append(":woman_artist: ").append("Мастер: ").append(master.getName()).append("\n")
                .append(":calendar: ").append("Дата: ").append(date.toString()).append("\n")
                .append(":mantelpiece_clock: ").append("Время: ").append(workTime.getDescription());

        sendMessage.setText(convertToEmoji(text.toString()));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        InlineKeyboardButton approveButton = new InlineKeyboardButton();
        approveButton.setText(convertToEmoji(":white_check_mark: Подтверждаю"));
        approveButton.setCallbackData("approve");
        List<InlineKeyboardButton> approveButtonRow = new ArrayList<>();
        approveButtonRow.add(approveButton);
        keyboard.add(approveButtonRow);

        addBackBookStageButton(keyboard, "backToTime");
        addMainMenuButton(keyboard);

        markup.setKeyboard(keyboard);
        sendMessage.setReplyMarkup(markup);;

        return sendMessage;
    }

    public EditMessageText bookService(Long chatId, BookingState state, Long messageId) {
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(chatId);

        if (messageId != null) {
            editMessageText.setMessageId(messageId.intValue());
        }

        Client client = clientService.findByChatId(chatId);
        Salon clientSalon = client.getSalon();
        StringBuilder text = new StringBuilder();

        if (!state.checkService()) {
            text.append(":point_down: Выберите услугу :point_down:\n\n");
            List<Services> allServices = serviceService.findBySalons(clientSalon);

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

            for (Services service : allServices) {

                text.append(":bell: ").append("Название: ").append(service.getName()).append("\n")
                        .append(":hourglass: ").append("Длительнсть: ").append(convertMinutesToHours(service.getDuration())).append("\n")
                        .append(":money_with_wings: ").append("Цена: ").append(service.getPrice()).append(" nis\n")
                        .append(":memo: ").append("Описание: ").append(service.getDescription()).append("\n\n");

                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(convertToEmoji(":fleur_de_lis:" + service.getName()));
                button.setCallbackData("service_" + service.getName());

                List<InlineKeyboardButton> row = new ArrayList<>();
                row.add(button);

                keyboard.add(row);
            }

            addMainMenuButton(keyboard);

            markup.setKeyboard(keyboard);
            editMessageText.setReplyMarkup(markup);

            editMessageText.setText(convertToEmoji(text.toString()));

            return editMessageText;
        }

        if (!state.checkMaster()) {

            text.append(":star: Вы выбрали:\n")
                    .append(":bell: ").append("Услуга: ").append(state.getService().getName()).append("\n\n");

            text.append(":woman_artist: Выберите мастера :point_down:\n\n");
            List<Master> allMasters = masterService.findByServicesContainingAndSalon(state.getService(), clientSalon);

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

            for (Master master : allMasters) {
                text.append(":woman_artist: ").append("Мастер: ").append(master.getName()).append("\n")
                        .append(":star: ").append("Рейтинг: ").append(masterReviewService.getRatingByMaster(master)).append("\n")
                        .append(":memo: ").append("Описание: ").append(master.getName()).append("\n");

                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(convertToEmoji(":star: " + master.getName()));
                button.setCallbackData("master_" + master.getId());

                List<InlineKeyboardButton> row = new ArrayList<>();
                row.add(button);

                keyboard.add(row);
            }

            addBackBookStageButton(keyboard, "backToServices");
            addMainMenuButton(keyboard);

            markup.setKeyboard(keyboard);
            editMessageText.setReplyMarkup(markup);

            editMessageText.setText(convertToEmoji(text.toString()));

            return editMessageText;
        }

        if (!state.checkDate()) {
            text.append(":star: Вы выбрали:\n")
                    .append(":bell: ").append("Услуга: ").append(state.getService().getName()).append("\n")
                    .append(":woman_artist: ").append("Мастер: ").append(state.getMaster().getName()).append("\n\n");

            text.append(":calendar: Выберите дату :point_down:\n");

            editMessageText.setText(convertToEmoji(text.toString()));
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

            addBackBookStageButton(keyboard, "backToMasters");
            addMainMenuButton(keyboard);

            markup.setKeyboard(keyboard);
            editMessageText.setReplyMarkup(markup);

            return editMessageText;
        }

        if (!state.checkTime()) {
            int duration = state.getService().getDuration();
            LocalDate chosenDate = state.getDate();

            text.append(":star: Вы выбрали:\n")
                    .append(":bell: ").append("Услуга: ").append(state.getService().getName()).append("\n")
                    .append(":woman_artist: ").append("Мастер: ").append(state.getMaster().getName()).append("\n")
                    .append(":calendar: ").append("Дата: ").append(chosenDate).append("\n\n");
            text.append(":mantelpiece_clock: Выберите время :point_down:\n");

            editMessageText.setText(convertToEmoji(text.toString()));

            List<Appointment> appointments = appointmentService.getAppointmentsByDateAndMaster(chosenDate, state.getMaster());

            LocalDateTime currentDateTime = LocalDateTime.now();
            int currentHour = currentDateTime.getHour();
            int currentMinute = currentDateTime.getMinute();

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

            int buttonsInRow = 3;
            List<InlineKeyboardButton> row = new ArrayList<>();

            // Создадим множество для хранения временных слотов, занятых забронированными услугами
            Set<Integer> occupiedSlots = new HashSet<>();

            // Заполним множество занятыми временными слотами для выбранной даты
            for (Appointment appointment : appointments) {
                int startSlot = appointment.getAppointmentTime().ordinal();
                int endSlot = startSlot + appointment.getService().getDuration() / 15;
                for (int i = startSlot; i < endSlot; i++) {
                    occupiedSlots.add(i);
                }
            }

            // Пройдемся по временным слотам с шагом, соответствующим длительности услуги
            for (int i = 0; i < WorkTime.values().length - duration / 15 + 1; i += duration / 15) {
                int workTimeValue = WorkTime.values()[i].ordinal();
                boolean isTimeFit = true; // Предполагаем, что время подходит

                // Проверяем, не прошло ли время и не занято ли оно, а также соответствует ли выбранная дата
                if (!(chosenDate.isEqual(LocalDate.now()) && workTimeValue <= currentHour - 9)) {
                    // Проверяем, не пересекается ли интервал с уже занятыми временными слотами
                    for (int j = 0; j < duration / 15; j++) {
                        int index = i + j;
                        if (index >= WorkTime.values().length || occupiedSlots.contains(index)) {
                            isTimeFit = false;
                            break;
                        }
                    }

                    // Если время подходит, добавляем кнопку для выбора
                    if (isTimeFit) {
                        InlineKeyboardButton button = new InlineKeyboardButton();
                        // Формируем текст кнопки с интервалом времени
                        button.setText(WorkTime.values()[i].getDescription() + " - " + WorkTime.values()[i + duration / 15].getDescription());
                        button.setCallbackData("time_" + WorkTime.values()[i].getDescription());

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

            addBackBookStageButton(keyboard, "backToDate");
            addMainMenuButton(keyboard);

            markup.setKeyboard(keyboard);
            editMessageText.setReplyMarkup(markup);

            return editMessageText;
        }

        return null;
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

    public EditMessageText approveCancel(Long chatId, Long appointmentId, Long messageId) {
        EditMessageText sendMessage = new EditMessageText();
        sendMessage.setChatId(chatId);

        if (messageId != null) {
            sendMessage.setMessageId(messageId.intValue());
        }

        sendMessage.setText(convertToEmoji(":thinking: Вы действительно хотите отменить услугу :thinking:\n\n"));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        InlineKeyboardButton yesButton = new InlineKeyboardButton();
        yesButton.setText(convertToEmoji(":white_check_mark: Да"));
        yesButton.setCallbackData("approveCancel_" + appointmentId);
        List<InlineKeyboardButton> yesButtonRow = new ArrayList<>();
        yesButtonRow.add(yesButton);
        keyboard.add(yesButtonRow);

        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(convertToEmoji(":x: Нет"));
        backButton.setCallbackData("menu");
        List<InlineKeyboardButton> backButtonRow = new ArrayList<>();
        backButtonRow.add(backButton);
        keyboard.add(backButtonRow);

        markup.setKeyboard(keyboard);
        sendMessage.setReplyMarkup(markup);

        return sendMessage;
    }

    public EditMessageText cancelService(Long chatId, Long messageId) {
        EditMessageText sendMessage = new EditMessageText();
        sendMessage.setChatId(chatId);

        if (messageId != null) {
            sendMessage.setMessageId(messageId.intValue());
        }

        int counter = 1;

        Client client = clientService.findByChatId(chatId);
        List<Appointment> appointmentsByClient = appointmentService.getAppointmentsByClient(client);

        StringBuilder messageText = new StringBuilder(convertToEmoji(":memo: Ваши забронированные услуги :point_down:\n\n"));
        for (Appointment appointment : appointmentsByClient) {
            if (appointment.getStatus() == AppointmentStatus.BANNED) {
                messageText
                        .append(":hash: Номер: ").append(counter++).append("\n")
                        .append(":bell: Услуга: " + appointment.getService().getName()).append("\n")
                        .append(":woman_artist: Мастер: " + appointment.getMaster().getName()).append("\n")
                        .append(":calendar: Дата: " + appointment.getAppointmentDate()).append("\n")
                        .append(":mantelpiece_clock:: Время: " + appointment.getAppointmentTime().getDescription()).append("\n")
                        .append(":money_with_wings: Цена: " + appointment.getService().getPrice()).append("nis \n\n");
            }
        }

        sendMessage.setText(convertToEmoji(messageText.toString()));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        counter = 1;

        for (Appointment appointment : appointmentsByClient) {
            if (appointment.getStatus() == AppointmentStatus.BANNED) {
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(convertToEmoji(":x: Отменить " + counter));
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
        if (!isAuthorized(chatId)) {
            return sendNonAuthorizedMessage(chatId);
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(convertToEmoji(":point_down: Выберите действие из меню :point_down:"));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton button1 = new InlineKeyboardButton();
        button1.setText(convertToEmoji(":calendar: Запись на услугу"));
        button1.setCallbackData("bookService");
        row1.add(button1);
        keyboard.add(row1);

        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(convertToEmoji(":mag_right: Мои записи"));
        button.setCallbackData("myServices");
        row.add(button);
        keyboard.add(row);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton button3 = new InlineKeyboardButton();
        button3.setText(convertToEmoji(":scroll: Навигация"));
        button3.setCallbackData("navigation");
        row3.add(button3);
        keyboard.add(row3);

        List<InlineKeyboardButton> row5 = new ArrayList<>();
        InlineKeyboardButton button5 = new InlineKeyboardButton();
        button5.setText(convertToEmoji(":oncoming_taxi: Добраться до салона"));
        button5.setCallbackData("wayToSalon");
        row5.add(button5);
        keyboard.add(row5);

        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);

        return message;
    }

    public EditMessageText menu(Long chatId, Long messageId) {
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId);

        if (messageId != null) {
            message.setMessageId(messageId.intValue());
        }

        message.setText(convertToEmoji(":point_down: Выберите действие из меню :point_down:"));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton button1 = new InlineKeyboardButton();
        button1.setText(convertToEmoji(":calendar: Запись на услугу"));
        button1.setCallbackData("bookService");
        row1.add(button1);
        keyboard.add(row1);

        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(convertToEmoji(":mag_right: Мои записи"));
        button.setCallbackData("myServices");
        row.add(button);
        keyboard.add(row);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton button3 = new InlineKeyboardButton();
        button3.setText(convertToEmoji(":scroll: Навигация"));
        button3.setCallbackData("navigation");
        row3.add(button3);
        keyboard.add(row3);

        List<InlineKeyboardButton> row5 = new ArrayList<>();
        InlineKeyboardButton button5 = new InlineKeyboardButton();
        button5.setText(convertToEmoji(":oncoming_taxi: Добраться до салона"));
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

            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(convertToEmoji(":house_with_garden: Главное меню"));
            button.setCallbackData("menuSendMessage");
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            keyboard.add(row);

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
            sendMessage = inputPhoneNumberMessage(update.getMessage().getChatId());
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
        button.setText(convertToEmoji(":house_with_garden: Главное меню "));
        button.setCallbackData("menu");
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(button);
        keyboard.add(row);
    }

    private void addBackBookStageButton(List<List<InlineKeyboardButton>> keyboard, String buttonName){
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(convertToEmoji(":arrow_left: Назад "));
        button.setCallbackData(buttonName);
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(button);
        keyboard.add(row);
    }

    private void addNextButton(List<List<InlineKeyboardButton>> keyboard) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(convertToEmoji(convertToEmoji(":arrow_right: Далее ")));
        button.setCallbackData("next");
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(button);
        keyboard.add(row);
    }

    private String convertToEmoji(String text) {
        String s = EmojiParser.parseToUnicode(text);
        return s;
    }

    public boolean isValidPhoneNumber(String phoneNumber) {
        if (!phoneNumber.startsWith("+")) {
            return false;
        }

        String digitsOnly = phoneNumber.substring(1);

        if (!digitsOnly.matches("\\d+")) {
            return false;
        }

        return true;
    }

    public String convertMinutesToHours(int minutes) {
        int hours = minutes / 60;
        int remainingMinutes = minutes % 60;

        if (hours > 0 && remainingMinutes > 0) {
            return hours + "ч " + remainingMinutes + "мин";
        } else if (hours > 0) {
            return hours + "ч";
        } else {
            return remainingMinutes + "мин";
        }
    }

    public boolean isAuthorized(Long chatId) {
        return clientService.existsByChatId(chatId);
    }

    public SendMessage sendNonAuthorizedMessage(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(convertToEmoji(":warning:Вы не зарегестрированы! Чтобы зарегестрироваться выполните команду /start."));

        return sendMessage;
    }
}
