package org.xtinastudio.com.tg.bots.clientbot.service;

import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendLocation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.xtinastudio.com.entity.*;
import org.xtinastudio.com.enums.AppointmentStatus;
import org.xtinastudio.com.enums.Language;
import org.xtinastudio.com.enums.WorkStatus;
import org.xtinastudio.com.enums.WorkTime;
import org.xtinastudio.com.service.*;
import org.xtinastudio.com.tg.bots.masterbot.service.MasterNotice;
import org.xtinastudio.com.tg.properties.ClientBotProperties;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;


@Slf4j
@Component
public class ClientBot extends TelegramLongPollingBot {

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

    private BookingState bookingState = new BookingState();

    private RatingState ratingState = new RatingState();

    private Long canceledAppointment = null;

    private final ClientBotProperties botProperties;

    public ClientBot(ClientBotProperties botProperties) {
        this.botProperties = botProperties;

        List<BotCommand> commandList = new ArrayList<>();
        commandList.add(new BotCommand("/start", "Начать использовать бот"));
        commandList.add(new BotCommand("/menu", "Главное меню"));
        commandList.add(new BotCommand("/instruction", "Инструкция пользования ботом"));
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
        BotApiMethod<?> message = mainCommands(update);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Some error with answer:" + e.getMessage());
        }
    }

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
                    deleteMessageById(chatId.toString(), messageId.intValue());
                    sendMessage = menu(chatId);
                    bookingState = new BookingState();
                    return sendMessage;
                case "menu":
                    editMessage = menu(chatId, messageId);
                    bookingState = new BookingState();
                    return editMessage;
                case "next":
                    editMessage = selectSalon(chatId, messageId);
                    bookingState = new BookingState();
                    return editMessage;
                case "wayToSalon":
                    deleteMessageById(chatId.toString(), messageId.intValue());
                    sendLocation = sendSalonLocation(chatId);
                    return sendLocation;
                case "bookService":
                    editMessage = bookService(chatId, bookingState, messageId);
                    return editMessage;
                case "aboutSalon":
                    editMessage = aboutSalon(chatId, messageId);
                    return editMessage;
                case "ourMasters":
                    editMessage = aboutMasters(chatId, messageId);
                    return editMessage;
                case "serviceKind":
                    String serviceKind = getDataCallbackQuery(data, 1);
                    bookingState.setServiceKind(serviceKind);
                    editMessage = bookService(chatId, bookingState, messageId);
                    return editMessage;
                case "service":
                    String service = getDataCallbackQuery(data, 1);
                    Services serviceByName = serviceService.findByName(service);
                    bookingState.setService(serviceByName);
                    editMessage = bookService(chatId, bookingState, messageId);
                    return editMessage;
                case "usualTime":
                    bookingState.setIndividualTime(false);
                    editMessage = bookService(chatId, bookingState, messageId);
                    return editMessage;
                case "individualTime":
                    bookingState.setIndividualTime(true);
                    editMessage = bookService(chatId, bookingState, messageId);
                    return editMessage;
                case "duration":
                    int duration = Integer.parseInt(getDataCallbackQuery(data, 1));
                    bookingState.setDuration(duration);
                    editMessage = bookService(chatId, bookingState, messageId);
                    return editMessage;
                case "master":
                    String master = getDataCallbackQuery(data, 1);
                    Master masterById = masterService.findById(Long.parseLong(master));
                    bookingState.setMaster(masterById);
                    editMessage = bookService(chatId, bookingState, messageId);
                    return editMessage;
                case "date":
                    String date = getDataCallbackQuery(data, 1);
                    LocalDate localDate = LocalDate.parse(date);
                    bookingState.setDate(localDate);
                    editMessage = bookService(chatId, bookingState, messageId);
                    return editMessage;
                case "time":
                    String time = getDataCallbackQuery(data, 1);
                    WorkTime workTime = parseWorkTime(time);
                    bookingState.setWorkTime(workTime);
                    editMessage = approveBookingService(chatId, bookingState, messageId);
                    return editMessage;
                case "approve":
                    Appointment appointment = new Appointment();
                    appointment.setService(bookingState.getService());
                    appointment.setMaster(bookingState.getMaster());
                    appointment.setAppointmentDate(bookingState.getDate());
                    appointment.setAppointmentTime(bookingState.getWorkTime());
                    appointment.setClient(clientService.findByChatId(chatId));
                    appointment.setStatus(AppointmentStatus.BANNED);

                    if (bookingState.getIndividualTime()) {
                        appointment.setDuration(bookingState.getDuration());
                    }

                    appointmentService.create(appointment);
                    bookingState = new BookingState();
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
                case "backToServiceKind":
                    bookingState.setServiceKind(null);
                    editMessage = bookService(chatId, bookingState, messageId);
                    return editMessage;
                case "backToServices":
                    bookingState.setService(null);
                    editMessage = bookService(chatId, bookingState, messageId);
                    return editMessage;
                case "backToIndividualTime":
                    bookingState.setIndividualTime(false);
                    editMessage = bookService(chatId, bookingState, messageId);
                    return editMessage;
                case "backToSelectIndividualTime":
                    bookingState.setDuration(null);
                    editMessage = bookService(chatId, bookingState, messageId);
                    return editMessage;
                case "backToMasters":
                    bookingState.setMaster(null);
                    editMessage = bookService(chatId, bookingState, messageId);
                    return editMessage;
                case "backToDate":
                    bookingState.setDate(null);
                    editMessage = bookService(chatId, bookingState, messageId);
                    return editMessage;
                case "backToTime":
                    bookingState.setWorkTime(null);
                    editMessage = bookService(chatId, bookingState, messageId);
                    return editMessage;
                case "backToCalendar":
                    editMessage = bookService(chatId, bookingState, messageId);
                    return editMessage;
                case "previousMonth":
                    LocalDate previousMonth = bookingState.getSelectMonth();
                    bookingState.setSelectMonth(previousMonth.minusMonths(1));
                    editMessage = bookService(chatId, bookingState, messageId);
                    return editMessage;
                case "nextMonth":
                    LocalDate nextMonth = bookingState.getSelectMonth();
                    bookingState.setSelectMonth(nextMonth.plusMonths(1));
                    editMessage = bookService(chatId, bookingState, messageId);
                    return editMessage;
                case "checkRating":
                    int checkRating = Integer.parseInt(getDataCallbackQuery(data, 1));
                    ratingState.setMasterRating(checkRating);
                    editMessage = approveCheckRating(chatId, messageId);
                    return editMessage;
                case "backToSendCheckRating":
                    editMessage = sendCheckRating(chatId, messageId, ratingState.getAppointment());
                    return editMessage;
                case "approveCheckRating":
                    try {
                        deleteMessageById(chatId.toString(), messageId.intValue());
                        MasterReview masterReview = new MasterReview();
                        masterReview.setClient(clientService.findByChatId(chatId));
                        masterReview.setMaster(ratingState.getMaster());
                        masterReview.setRating(ratingState.getMasterRating());
                        masterReview.setReviewDate(LocalDate.now().atStartOfDay());
                        masterReviewService.create(masterReview);
                        break;
                    } catch (Exception e){
                        System.out.println("Exception with answer: " + e.getMessage());
                    }
                case "cancelNoticeDeleteMessage":
                    deleteMessageById(chatId.toString(), messageId.intValue());
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

        text.append(":scroll: Инструкция\n\n").append(":one: Обратите внимание, что слева внизу от клавиатуры есть встроенное 'menu' c помощью которого " + "Вы можете выполнять различные команды напрямую.").append("\n\n")
                .append(":two: Команды встроенного меню:").append("\n")
                .append(" :two::one: Команда /start предназначена для регистрации.").append("\n")
                .append(" :two::two: Команда /menu вызвает главное меню, в котором можно делать/отменять записи и тд.").append("\n")
                .append(" :two::three: Команда /instruction выдаёт инструкцию, как пользоваться этим ботом").append("\n")
                .append(" :two::four: Команда /reentry_phone_number позволяет ввести новый номер телефона, чтобы использовать его вместо старого").append("\n")
                .append(" :two::five: Команда /change_salon позволяет поменять салон для бронирования услуг").append("\n\n")
                .append(":three: Команды главного меню:")
                .append("\n").append(" :three::one: С помощью 'Запись на услугу' Вы можете записаться на услугу").append("\n")
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

        text.append(":scroll: Инструкция\n\n").append(":one: Обратите внимание, что слева внизу от клавиатуры есть встроенное 'menu' c помощью которого " + "Вы можете выполнять различные команды напрямую.").append("\n\n").append(":two: Команды встроенного меню:").append("\n").append(" :two::one: Команда /start предназначена для регистрации.").append("\n").append(" :two::two: Команда /menu вызвает главное меню, в котором можно делать/отменять записи и тд.").append("\n").append(" :two::three: Команда /instruction выдаёт инструкцию, как пользоваться этим ботом").append("\n").append(" :two::four: Команда /reentry_phone_number позволяет ввести новый номер телефона, чтобы использовать его вместо старого").append("\n").append(" :two::five: Команда /change_salon позволяет поменять салон для бронирования услуг").append("\n\n").append(":three: Команды главного меню:").append("\n").append(" :three::one: С помощью 'Запись на услугу' Вы можете записаться на услугу").append("\n").append(" :three::two: С помощью 'Мои записи' Вы можете просмотреть действующие записи и в случае чего отменить их.").append("\n").append(" :three::three: В разделе 'Навигация' Вы можете найти информацию о салоне, мастерах и инструкцию").append("\n").append(" :three::four: С помощью 'Добраться до Салона' Вы можете нажав на карту - проложить путь до салона через навигатор").append("\n");

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
            text.append(":woman_artist: ").append("Мастер: ").append(master.getName()).append("\n").append(":star: ").append("Рейтинг: ").append(roundToTwoDecimalPlaces(masterReviewService.getMasterRating(master))).append("\n").append(":link: ").append("Страничка: ").append(master.getUrl()).append("\n").append(":memo: ").append("Описание: ").append(master.getName()).append("\n");
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

        editMessageText.setText(convertToEmoji(":tada: Поздравляем с успешной регистрацией! :tada: \n" + "Теперь вы часть нашего сообщества. Добро пожаловать!\n" + ":point_down: Чтобы продолжить, воспользуйтесь главным меню :point_down:"));

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
            editMessageText.setText(convertToEmoji(":star: Добро пожаловать в чат-бот салона красоты xtina.studio! :star:\n" + "Сейчас Вам будет нужно пройти небольшую регистрацию, после чего вы сможете полльзоваться ботом!\n" + ":telephone: Для начала введите свой номер телефона.\n" + "Без пробелов и тире с помощью клавиатуры (+123456789)\n"));
        } else {
            editMessageText.setText(convertToEmoji(":telephone: Введите новый номер телефона.\n" + "Без пробелов и тире с помощью клавиатуры (+123456789)\n"));
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
            editMessageText.setText(convertToEmoji(":star: Отлично! :star:\n" + ":telephone: Ваш номер телефона сохранён!"));

            addNextButton(keyboard);
        } else {
            client.setPhoneNumber(phoneNumber);
            clientService.editById(client.getId(), client);
            editMessageText.setText(convertToEmoji(":star: Ваш номер успешно изменён! :star:\n" + "Можете продолжить пользоваться ботом!"));

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
                int duration;
                if (appointment.getDuration() != null) {
                    duration = appointment.getDuration();
                } else {
                    duration = appointment.getService().getDuration();
                }

                messageText.append(":bell: ").append("Услуга: " + appointment.getService().getName()).append("\n")
                        .append(":cherry_blossom: ").append("Вид услуги: ").append(appointment.getService().getKind()).append("\n")
                        .append(":woman_artist: ").append("Мастер: " + appointment.getMaster().getName()).append("\n")
                        .append(":calendar: ").append("Дата: " + appointment.getAppointmentDate()).append("\n")
                        .append(":mantelpiece_clock: ").append("Время: " + appointment.getAppointmentTime().getDescription()).append("\n")
                        .append(":hourglass: ").append("Продолжительность: " + convertMinutesToHours(duration)).append("\n")
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

        int duration;

        if (state.getDuration() != null) {
            duration = state.getDuration();
        } else {
            duration = state.getService().getDuration();
        }

        StringBuilder text = new StringBuilder();
        text.append("Подтвердите выбор услуги:\n")
                .append(":cherry_blossom: ").append("Вид услуги: ").append(service.getKind()).append("\n")
                .append(":bell: ").append("Услуга: ").append(service.getName()).append("\n")
                .append(":woman_artist: ").append("Мастер: ").append(master.getName()).append("\n")
                .append(":calendar: ").append("Дата: ").append(date.toString()).append("\n")
                .append(":mantelpiece_clock: ").append("Время: ").append(workTime.getDescription()).append("\n")
                .append(":hourglass: ").append("Продолжительность: " + convertMinutesToHours(duration));


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
        sendMessage.setReplyMarkup(markup);
        ;

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

        if (!state.checkServiceKind()) {
            text.append("Выберите вид услуги:\n");
            List<Services> allServices = serviceService.findBySalons(clientSalon);

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

            for (Services service : allServices) {
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(convertToEmoji(":fleur_de_lis:" + service.getKind()));
                button.setCallbackData("serviceKind_" + service.getKind());

                List<InlineKeyboardButton> row = new ArrayList<>();
                row.add(button);

                if (!service.getKind().equals("holiday")) {
                    keyboard.add(row);
                }
            }

            addMainMenuButton(keyboard);

            markup.setKeyboard(keyboard);
            editMessageText.setReplyMarkup(markup);

            editMessageText.setText(convertToEmoji(text.toString()));

            return editMessageText;
        }

        if (!state.checkService()) {
            text.append("Вы выбрали:\n").append(":cherry_blossom: ").append("Вид услуги: ").append(state.getServiceKind()).append("\n\n");

            text.append("Выберите услугу:\n");
            List<Services> allServices = serviceService.findBySalonsAndKind(clientSalon, state.getServiceKind());

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

            for (Services service : allServices) {

                text.append(":bell: ").append("Название: ").append(service.getName()).append("\n").append(":hourglass: ").append("Длительнсть: ").append(convertMinutesToHours(service.getDuration())).append("\n").append(":money_with_wings: ").append("Цена: ").append(service.getPrice()).append(" nis\n").append(":memo: ").append("Описание: ").append(service.getDescription()).append("\n\n");
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(convertToEmoji(":fleur_de_lis:" + service.getName()));
                button.setCallbackData("service_" + service.getName());

                List<InlineKeyboardButton> row = new ArrayList<>();

                row.add(button);
                keyboard.add(row);
            }

            addBackBookStageButton(keyboard,"backToServiceKind");
            addMainMenuButton(keyboard);

            markup.setKeyboard(keyboard);
            editMessageText.setReplyMarkup(markup);

            editMessageText.setText(convertToEmoji(text.toString()));

            return editMessageText;
        }

        if (!state.checkIndividualTime()) {
            text.append("Вы выбрали:\n")
                    .append(":cherry_blossom: ").append("Вид услуги: ").append(state.getServiceKind()).append("\n")
                    .append(":bell: ").append("Услуга: ").append(state.getService().getName()).append("\n\n");

            text.append(":bookmark: Если Вы консультировались с мастером по поводу продолжительности процедуры и Вам нужно меньше/больше времени на процедуру, то Вам следует выбрать индивидуальное время.\n");
            text.append(":bookmark: Если Вы не консультировались с мастером, то выбирайте обыное время.\n\n");

            text.append(":light_bulb: Нужна ли Вам индивидуальная длительность процедуры?\n\n");

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(convertToEmoji(":fleur_de_lis:" + "Да, нужна"));
            button.setCallbackData("individualTime_");
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            keyboard.add(row);

            InlineKeyboardButton button1 = new InlineKeyboardButton();
            button1.setText(convertToEmoji(":fleur_de_lis:" + "Нет, не нужна"));
            button1.setCallbackData("usualTime_");
            List<InlineKeyboardButton> row2 = new ArrayList<>();
            row2.add(button1);
            keyboard.add(row2);

            List<InlineKeyboardButton> row3 = new ArrayList<>();
            InlineKeyboardButton button4 = new InlineKeyboardButton();
            button4.setText(convertToEmoji(":arrow_left: Назад "));
            button4.setCallbackData("backToIndividualTime");
            row3.add(button4);

            InlineKeyboardButton button3 = new InlineKeyboardButton();
            button3.setText(convertToEmoji(":house_with_garden: Главное меню "));
            button3.setCallbackData("menu");
            row3.add(button3);

            keyboard.add(row3);

            markup.setKeyboard(keyboard);
            editMessageText.setReplyMarkup(markup);

            editMessageText.setText(convertToEmoji(text.toString()));

            return editMessageText;
        }

        if (state.getIndividualTime()) {
            if (!state.checkDuration()) {
                text.append("Вы выбрали:\n")
                        .append(":cherry_blossom: ").append("Вид услуги: ").append(state.getServiceKind()).append("\n")
                        .append(":bell: ").append("Услуга: ").append(state.getService().getName()).append("\n")
                        .append(":hourglass:").append("Продолжительность: ").append("индивидуальная\n\n");

                text.append("Выберете индивидуальную прододжительность для услуги, которую Вам сказал мастрер после консультации:\n\n");

                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

                List<InlineKeyboardButton> row = new ArrayList<>();

                for (int i = 15; i <= 180; i += 15) {
                    InlineKeyboardButton button = new InlineKeyboardButton();
                    button.setText(convertMinutesToHours(i));
                    button.setCallbackData("duration_" + i);

                    row.add(button);
                    if (row.size() == 3) {
                        keyboard.add(row);
                        row = new ArrayList<>();
                    }
                }
                if (!row.isEmpty()) {
                    keyboard.add(row);
                }

                addBackBookStageButton(keyboard, "backToIndividualTime");
                addMainMenuButton(keyboard);

                markup.setKeyboard(keyboard);
                editMessageText.setReplyMarkup(markup);

                editMessageText.setText(convertToEmoji(text.toString()));

                return editMessageText;
            }
        }

        if (!state.checkMaster()) {
            text.append("Вы выбрали:\n")
                    .append(":cherry_blossom: ").append("Вид услуги: ").append(state.getServiceKind()).append("\n")
                    .append(":bell: ").append("Услуга: ").append(state.getService().getName()).append("\n\n");

            text.append("Выберите мастера:\n");
            List<Master> allMasters = masterService.findByServicesContainingAndSalon(state.getService(), clientSalon);

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

            for (Master master : allMasters) {
                text.append(":woman_artist: ").append("Мастер: ").append(master.getName()).append("\n").append(":scroll: ").append("Статус: ").append(master.getWorkStatus().getDescription()).append("\n").append(":star: ").append("Рейтинг: ").append(roundToTwoDecimalPlaces(masterReviewService.getMasterRating(master))).append("\n").append(":memo: ").append("Описание: ").append(master.getName()).append("\n");

                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(convertToEmoji(":star: " + master.getName()));
                button.setCallbackData("master_" + master.getId());

                List<InlineKeyboardButton> row = new ArrayList<>();
                row.add(button);

                keyboard.add(row);
            }

            addBackBookStageButton(keyboard, "backToIndividualTime");
            addMainMenuButton(keyboard);

            markup.setKeyboard(keyboard);
            editMessageText.setReplyMarkup(markup);

            editMessageText.setText(convertToEmoji(text.toString()));

            return editMessageText;
        }

        if (!state.checkDate()) {
            text.append("Вы выбрали:\n")
                    .append(":cherry_blossom: ").append("Вид услуги: ").append(state.getServiceKind()).append("\n")
                    .append(":bell: ").append("Услуга: ").append(state.getService().getName()).append("\n")
                    .append(":woman_artist: ").append("Мастер: ").append(state.getMaster().getName()).append("\n\n");

            text.append(":palm_tree:").append(" - у мастера выходной").append("\n")
                    .append(":hospital:").append(" - у мастера больничный").append("\n")
                    .append(":airplane:").append(" - у мастера отпуск").append("\n\n");

            text.append("Выберите дату:\n");

            editMessageText.setText(convertToEmoji(text.toString()));
            List<LocalDate> allDates = getAvailableDates(60);

            InlineKeyboardMarkup calendarMarkup = createCalendar(state.getMaster(), state.getSelectMonth(), allDates);

            editMessageText.setReplyMarkup(calendarMarkup);

            return editMessageText;
        }

        if (!state.checkTime()) {
            int duration;

            if (state.getIndividualTime()) {
                duration = state.getDuration();
            } else {
                duration = state.getService().getDuration();
            }

            LocalDate chosenDate = state.getDate();

            text.append(":star: Вы выбрали:\n")
                    .append(":cherry_blossom: ").append("Вид услуги: ").append(state.getServiceKind()).append("\n")
                    .append(":bell: ").append("Услуга: ").append(state.getService().getName()).append("\n")
                    .append(":woman_artist: ").append("Мастер: ").append(state.getMaster().getName()).append("\n")
                    .append(":calendar: ").append("Дата: ").append(chosenDate).append("\n\n");
            text.append("Выберите время :point_down:\n");

            editMessageText.setText(convertToEmoji(text.toString()));

            List<Appointment> appointments = appointmentService.getAppointmentsByDateAndMaster(chosenDate, state.getMaster());

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

            int buttonsInRow = 3;
            List<InlineKeyboardButton> row = new ArrayList<>();

            HashMap<Integer, Integer> bookedServicePeriods = new HashMap<>();

            for (Appointment appointment : appointments) {
                if (appointment.getStatus() == AppointmentStatus.BANNED) {
                    int startDurationService = appointment.getAppointmentTime().ordinal();
                    int endDurationService;

                    if (appointment.getDuration() != null) {
                        endDurationService = startDurationService + appointment.getDuration() / 15;
                    } else {
                        endDurationService = startDurationService + appointment.getService().getDuration() / 15;
                    }

                    bookedServicePeriods.put(startDurationService, endDurationService);
                }
            }

            for (int i = 0; i < WorkTime.values().length - duration / 15; i += duration / 15) {
                int bookedTime = WorkTime.values()[i].ordinal();

                if (isNotOccupied(bookedServicePeriods, bookedTime, duration/15) && isTimePassed(WorkTime.values()[i].getDescription())) {
                    InlineKeyboardButton button = new InlineKeyboardButton();
                    button.setText(WorkTime.values()[i].getDescription() + " - " + WorkTime.values()[i + duration / 15].getDescription());
                    button.setCallbackData("time_" + WorkTime.values()[i].getDescription());

                    row.add(button);

                    if (row.size() == buttonsInRow) {
                        keyboard.add(row);
                        row = new ArrayList<>();
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

    private boolean isTimePassed(String timeStr) {
        if (LocalDate.now().equals(bookingState.getDate())) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("H:mm");
            LocalTime inputTime = LocalTime.parse(timeStr, formatter);
            LocalTime currentTime = LocalTime.now();
            return currentTime.isBefore(inputTime);
        } else {
            return true;
        }
    }

    public static double roundToTwoDecimalPlaces(double value) {
        BigDecimal bigDecimal = BigDecimal.valueOf(value);
        bigDecimal = bigDecimal.setScale(2, RoundingMode.HALF_UP);
        return bigDecimal.doubleValue();
    }

    private boolean isNotOccupied(HashMap<Integer, Integer> bookedServicePeriods, int bookedTime, int duration) {
        if (bookedServicePeriods.isEmpty()) {
            return true;
        }

        int bookedStartTime = bookedTime;
        int bookedEndTime = bookedStartTime + duration;

        for (Map.Entry<Integer, Integer> entry : bookedServicePeriods.entrySet()) {
            int startTime = entry.getKey();
            int endTime = entry.getValue();

            /*if (bookedTime <= endTime && bookedEndTime > startTime) {
                return false;
            }*/
            if (bookedTime < endTime && bookedEndTime > startTime) {
                return false;
            }
        }

        return true;
    }

    public List<LocalDate> getAvailableDates(int days) {
        List<LocalDate> availableDates = new ArrayList<>();
        LocalDate currentDate = LocalDate.now();

        if (currentDate.getDayOfWeek() == DayOfWeek.SATURDAY || currentDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            currentDate = currentDate.plusDays(1);
        }

        while (currentDate.getDayOfWeek() == DayOfWeek.SATURDAY || currentDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            currentDate = currentDate.plusDays(1);
        }

        availableDates.add(currentDate);

        int daysToAdd = 1;

        // Заполнение списка дат
        while (availableDates.size() < days) {
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

        sendMessage.setReplyMarkup(markup);
        markup.setKeyboard(keyboard);

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
                messageText.append(":hash: Номер: ").append(counter++).append("\n").append(":bell: Услуга: " + appointment.getService().getName()).append("\n").append(":woman_artist: Мастер: " + appointment.getMaster().getName()).append("\n").append(":calendar: Дата: " + appointment.getAppointmentDate()).append("\n").append(":mantelpiece_clock:: Время: " + appointment.getAppointmentTime().getDescription()).append("\n").append(":money_with_wings: Цена: " + appointment.getService().getPrice()).append("nis \n\n");
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

    private InlineKeyboardButton addMainMenuButton(List<List<InlineKeyboardButton>> keyboard) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(convertToEmoji(":house_with_garden: Главное меню "));
        button.setCallbackData("menu");
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(button);
        keyboard.add(row);
        return button;
    }

    private InlineKeyboardButton addBackBookStageButton(List<List<InlineKeyboardButton>> keyboard, String buttonName) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(convertToEmoji(":arrow_left: Назад "));
        button.setCallbackData(buttonName);
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(button);
        keyboard.add(row);
        return button;
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

    public void deleteMessageById(String chatId, int messageId) {
        DeleteMessage deleteMessage = new DeleteMessage(chatId, messageId);

        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            log.error("Some error with answer:" + e.getMessage());
        }
    }

    public InlineKeyboardMarkup createCalendar(Master master, LocalDate currentDate, List<LocalDate> availableDates) {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        int daysInMonth = currentDate.lengthOfMonth();
        int dayOfWeek = currentDate.withDayOfMonth(1).getDayOfWeek().getValue() % 7 + 1;
        int dayCounter = 1;

        List<InlineKeyboardButton> headerRow = new ArrayList<>();
        String monthYearText = currentDate.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + currentDate.getYear();
        InlineKeyboardButton headerButton = new InlineKeyboardButton();
        headerButton.setText(monthYearText);
        headerButton.setCallbackData("backToDate");
        headerRow.add(headerButton);
        keyboard.add(headerRow);

        List<InlineKeyboardButton> daysRow = new ArrayList<>();
        String[] daysOfWeek = {"Вс", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб"};
        for (String day : daysOfWeek) {
            InlineKeyboardButton dayButton = new InlineKeyboardButton();
            dayButton.setText(day);
            dayButton.setCallbackData("backToDate");
            daysRow.add(dayButton);
        }
        keyboard.add(daysRow);

        while (dayCounter <= daysInMonth) {
            List<InlineKeyboardButton> weekRow = new ArrayList<>();
            for (int i = 1; i <= 7; i++) {
                if (i < dayOfWeek || dayCounter > daysInMonth) {
                    InlineKeyboardButton emptyButton = new InlineKeyboardButton();
                    emptyButton.setText(" ");
                    emptyButton.setCallbackData("backToDate");
                    weekRow.add(emptyButton);
                } else {
                    LocalDate currentDateInLoop = LocalDate.of(currentDate.getYear(), currentDate.getMonth(), dayCounter);
                    InlineKeyboardButton dayButton = new InlineKeyboardButton();
                    if (availableDates.contains(currentDateInLoop)) {
                        List<Appointment> appointmentsByDateAndMaster = appointmentService.getAppointmentsByDateAndMaster(currentDateInLoop, master);

                        dayButton.setText(String.valueOf(dayCounter));
                        dayButton.setCallbackData("date_" + currentDateInLoop.toString());

                        if (!appointmentsByDateAndMaster.isEmpty()) {
                            if (appointmentsByDateAndMaster.get(0).getWorkStatus() == WorkStatus.SICK) {
                                dayButton.setText(convertToEmoji(String.valueOf(dayCounter) + ":hospital:"));
                                dayButton.setCallbackData("backToDate");
                            } else if (appointmentsByDateAndMaster.get(0).getWorkStatus() == WorkStatus.VACATION) {
                                dayButton.setText(convertToEmoji(String.valueOf(dayCounter) + ":airplane:"));
                                dayButton.setCallbackData("backToDate");
                            } else if (appointmentsByDateAndMaster.get(0).getWorkStatus() == WorkStatus.DAYOFF) {
                                dayButton.setText(convertToEmoji(String.valueOf(dayCounter) + ":palm_tree:"));
                                dayButton.setCallbackData("backToDate");
                            }
                        }
                    } else {
                        dayButton.setText(String.valueOf(dayCounter) + "❌");
                        dayButton.setCallbackData("backToDate");
                    }
                    weekRow.add(dayButton);
                    dayCounter++;
                }
            }
            keyboard.add(weekRow);
            dayOfWeek = 1;
        }

        List<InlineKeyboardButton> navRow = new ArrayList<>();
        InlineKeyboardButton prevButton = new InlineKeyboardButton();
        prevButton.setText(convertToEmoji(":arrow_backward:"));
        prevButton.setCallbackData("previousMonth_");
        navRow.add(prevButton);
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(convertToEmoji(":arrow_left: Назад"));
        backButton.setCallbackData("backToMasters_");
        navRow.add(backButton);
        InlineKeyboardButton menuButton = new InlineKeyboardButton();
        menuButton.setText(convertToEmoji(":house_with_garden: Меню"));
        menuButton.setCallbackData("menu_");
        navRow.add(menuButton);
        InlineKeyboardButton nextButton = new InlineKeyboardButton();
        nextButton.setText(convertToEmoji(":arrow_forward:"));
        nextButton.setCallbackData("nextMonth_");
        navRow.add(nextButton);
        keyboard.add(navRow);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    public EditMessageText sendCheckRating(Long chatId, Long messageId, Appointment appointment) {
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(chatId);
        editMessageText.setMessageId(messageId.intValue());
        StringBuilder text = new StringBuilder();

        Master master = appointment.getMaster();
        Services service = appointment.getService();
        Client client = appointment.getClient();

        ratingState.setAppointment(appointment);
        ratingState.setMaster(master);

        text.append("Поставьте оценку мастеру '").append(master.getName())
                .append("' за проделанную процедуру '").append(service.getName()).append("':\n");

        text.append("\nВы можете не ставить оценку мастеру нажав главное меню.\n\n");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(convertToEmoji(String.valueOf(i) + " :star:"));
            button.setCallbackData("checkRating_" + i);
            if (i <= 3) {
                row1.add(button);
            } else {
                row2.add(button);
            }
        }

        keyboard.add(row1);
        keyboard.add(row2);

        addMainMenuButton(keyboard);

        markup.setKeyboard(keyboard);
        editMessageText.setReplyMarkup(markup);

        editMessageText.setChatId(client.getChatId());
        editMessageText.setText(convertToEmoji(text.toString()));

        try {
            execute(editMessageText);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }

        return editMessageText;
    }

    public SendMessage sendCheckRating(Appointment appointment) {
        SendMessage sendMessage = new SendMessage();
        StringBuilder text = new StringBuilder();

        Master master = appointment.getMaster();
        Services service = appointment.getService();
        Client client = appointment.getClient();

        ratingState.setAppointment(appointment);
        ratingState.setMaster(master);

        text.append("Поставьте оценку мастеру '").append(master.getName())
                .append("' за проделанную процедуру '").append(service.getName()).append("':\n");

        text.append("\nВы можете не ставить оценку мастеру нажав главное меню.\n\n");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(convertToEmoji(String.valueOf(i) + " :star:"));
            button.setCallbackData("checkRating_" + i);
            if (i <= 3) {
                row1.add(button);
            } else {
                row2.add(button);
            }
        }

        keyboard.add(row1);
        keyboard.add(row2);

        addMainMenuButton(keyboard);

        markup.setKeyboard(keyboard);
        sendMessage.setReplyMarkup(markup);

        sendMessage.setChatId(client.getChatId());
        sendMessage.setText(convertToEmoji(text.toString()));

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }

        return sendMessage;
    }

    public SendMessage sendCancelNoticeToClient(Appointment appointment) {
        SendMessage sendMessage = new SendMessage();
        StringBuilder text = new StringBuilder();

        Master master = appointment.getMaster();
        Services service = appointment.getService();
        Client client = appointment.getClient();

        text.append("Мастер '").append(master.getName())
                .append("' отменил Вашу забронированную процедуру '").append(service.getName()).append("\n")
                .append("Для того чтобы узнать причину отмены свяжитесь мастером в его инстаграмм(Навигация -> Наши мастера)").append("'\n\n");

        text.append("Инстанрамм мастера: ").append(master.getUrl()).append("\n\n");

        text.append("После того как нажмёте 'Ок' сообщение исчезнет.");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();

        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(convertToEmoji("Ок"));
        button.setCallbackData("cancelNoticeDeleteMessage_");
        keyboard.add(row1);

        addMainMenuButton(keyboard);

        markup.setKeyboard(keyboard);
        sendMessage.setReplyMarkup(markup);

        sendMessage.setChatId(client.getChatId());
        sendMessage.setText(convertToEmoji(text.toString()));

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }

        return sendMessage;
    }

    public EditMessageText approveCheckRating(Long chatId, Long messageId) {
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(chatId);
        editMessageText.setMessageId(messageId.intValue());

        StringBuilder text = new StringBuilder();

        text.append("Подтвердите выбранную вами оценку для мастера\n\n");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        InlineKeyboardButton yesButton = new InlineKeyboardButton();
        yesButton.setText(convertToEmoji(":white_check_mark: Подтверждаю"));
        yesButton.setCallbackData("approveCheckRating_" + ratingState.getMasterRating());
        List<InlineKeyboardButton> yesButtonRow = new ArrayList<>();
        yesButtonRow.add(yesButton);
        keyboard.add(yesButtonRow);

        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(convertToEmoji(":arrow_backward: Назад"));
        backButton.setCallbackData("backToSendCheckRating_");
        List<InlineKeyboardButton> backButtonRow = new ArrayList<>();
        backButtonRow.add(backButton);
        keyboard.add(backButtonRow);

        editMessageText.setReplyMarkup(markup);
        markup.setKeyboard(keyboard);

        editMessageText.setText(convertToEmoji(text.toString()));
        return editMessageText;
    }
}
