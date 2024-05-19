package org.xtinastudio.com.tg.bots.masterbot.service;

import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendLocation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.xtinastudio.com.entity.*;
import org.xtinastudio.com.enums.AppointmentStatus;
import org.xtinastudio.com.enums.WorkStatus;
import org.xtinastudio.com.enums.WorkTime;
import org.xtinastudio.com.service.*;
import org.xtinastudio.com.tg.properties.MasterBotProperties;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
public class MasterBot extends TelegramLongPollingBot {

    @Autowired
    private MasterService masterService;

    @Autowired
    private SalonService salonService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private ServiceService serviceService;

    CalendarState calendarState = new CalendarState();

    RateState rateState = new RateState();

    HolidayState holidayState = new HolidayState();

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
        EditMessageText editMessageText = new EditMessageText();
        SendMessage sendMessage = new SendMessage();
        SendLocation sendLocation = new SendLocation();
        Long chatId = null;
        Long messageId = null;

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
                case "/salon_location":
                    sendLocation = sendSalonLocation(chatId);
                    return sendLocation;
                default:
                    if (checkLoginAndPasswordStructure(text)) {
                        String login = splitLoginAndPassword(text, 0);
                        String password = splitLoginAndPassword(text, 1);

                        sendMessage = authorizeMaster(chatId, login, password);
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
                    holidayState = new HolidayState();
                    sendMessage = menu(chatId);
                    return sendMessage;
                case "menu":
                    holidayState = new HolidayState();
                    editMessageText = menu(chatId, messageId);
                    return editMessageText;
                case "myServices":
                    editMessageText = selectServicePeriod(chatId, messageId);
                    return editMessageText;
                case "myServicesToday":
                    LocalDate current = LocalDate.now();
                    editMessageText = showBookedServices(chatId, messageId, current);
                    return editMessageText;
                case "myServicesTomorrow":
                    LocalDate tomorrow = LocalDate.now().plusDays(1);
                    editMessageText = showBookedServices(chatId, messageId, tomorrow);
                    return editMessageText;
                case "myServicesDate":
                    LocalDate selectDate = LocalDate.parse(getDataCallbackQuery(data, 1));
                    editMessageText = showBookedServices(chatId, messageId, selectDate);
                    return editMessageText;
                case "myServicesSelectDate":
                    editMessageText = selectDateByCalendar("myServicesDate", "myServicesPreviousMonth", "myServicesNextMonth", chatId, messageId);
                    return editMessageText;
                case "myServicesPreviousMonth":
                    LocalDate previousMonth = calendarState.getSelectMonth();
                    calendarState.setSelectMonth(previousMonth.minusMonths(1));
                    editMessageText = selectDateByCalendar("myServicesDate", "myServicesPreviousMonth", "myServicesNextMonth", chatId, messageId);
                    return editMessageText;
                case "myServicesNextMonth":
                    LocalDate nextMonth = calendarState.getSelectMonth();
                    calendarState.setSelectMonth(nextMonth.plusMonths(1));
                    editMessageText = selectDateByCalendar("myServicesDate", "myServicesPreviousMonth", "myServicesNextMonth", chatId, messageId);
                    return editMessageText;
                case "myServicesSelectRate":
                    String selectedServiceRate = getDataCallbackQuery(data, 1);
                    Appointment appointmentSelectedServiceRate = appointmentService.getById(Long.parseLong(selectedServiceRate));
                    rateState.setAppointment(appointmentSelectedServiceRate);
                    editMessageText = menuSelectRate(chatId, messageId);
                    return editMessageText;
                case "myServicesSelectRateEndService":
                    String selectedServiceRateEndService = getDataCallbackQuery(data, 1);
                    Appointment appointmentSelectedServiceRateEnd = appointmentService.getById(Long.parseLong(selectedServiceRateEndService));
                    editMessageText = endService(chatId, messageId, appointmentSelectedServiceRateEnd);
                    return editMessageText;
                case "myServicesSelectRateCancelService":
                    String selectedServiceRateCancelService = getDataCallbackQuery(data, 1);
                    Appointment appointmentSelectedServiceRateCancel = appointmentService.getById(Long.parseLong(selectedServiceRateCancelService));
                    editMessageText = cancelService(chatId, messageId, appointmentSelectedServiceRateCancel);
                    return editMessageText;
                case "myServicesSelectRateEndServiceApprove":
                    String appointmentId = getDataCallbackQuery(data, 1);
                    Appointment approve = appointmentService.getById(Long.parseLong(appointmentId));
                    approve.setStatus(AppointmentStatus.COMPLETED);
                    appointmentService.editById(approve.getId(), approve);
                    editMessageText = menu(chatId, messageId);
                    return editMessageText;
                case "myServicesSelectRateCancelServiceApprove":
                    String dataCallbackQuery = getDataCallbackQuery(data, 1);
                    Appointment cancel = appointmentService.getById(Long.parseLong(dataCallbackQuery));
                    cancel.setStatus(AppointmentStatus.CANCELED);
                    appointmentService.editById(cancel.getId(), cancel);
                    editMessageText = menu(chatId, messageId);
                    return editMessageText;
                case "myWorkTime":
                    editMessageText = managementWorkTime(chatId, messageId);
                    return editMessageText;
                case "myWorkTimeStatus":
                    String myWorkTimeStatusData = getDataCallbackQuery(data, 1);
                    WorkStatus workStatus = WorkStatus.valueOf(myWorkTimeStatusData);
                    holidayState.setWorkStatus(workStatus);
                    editMessageText = managementWorkTime(chatId, messageId);
                    return editMessageText;
                case "myWorkTimeStartDate":
                    LocalDate myWorkTimeStartDate = LocalDate.parse(getDataCallbackQuery(data, 1));
                    holidayState.setStartDate(myWorkTimeStartDate);
                    editMessageText = managementWorkTime(chatId, messageId);
                    return editMessageText;
                case "myWorkTimeStartDatePrev":
                    LocalDate myWorkTimeStartDatePrev = calendarState.getSelectMonth();
                    calendarState.setSelectMonth(myWorkTimeStartDatePrev.minusMonths(1));
                    editMessageText = managementWorkTime(chatId, messageId);
                    return editMessageText;
                case "myWorkTimeStartDateNext":
                    LocalDate myWorkTimeStartDateNext = calendarState.getSelectMonth();
                    calendarState.setSelectMonth(myWorkTimeStartDateNext.plusMonths(1));
                    editMessageText = managementWorkTime(chatId, messageId);
                    return editMessageText;
                case "myWorkTimeEndDate":
                    LocalDate myWorkTimeEndDate = LocalDate.parse(getDataCallbackQuery(data, 1));
                    holidayState.setEndDate(myWorkTimeEndDate);
                    editMessageText = managementWorkTime(chatId, messageId);
                    return editMessageText;
                case "myWorkTimeEndDatePrev":
                    LocalDate myWorkTimeEndDatePrev = calendarState.getSelectMonth();
                    calendarState.setSelectMonth(myWorkTimeEndDatePrev.minusMonths(1));
                    editMessageText = managementWorkTime(chatId, messageId);
                    return editMessageText;
                case "myWorkTimeEndDateNext":
                    LocalDate myWorkTimeEndDateNext = calendarState.getSelectMonth();
                    calendarState.setSelectMonth(myWorkTimeEndDateNext.plusMonths(1));
                    editMessageText = managementWorkTime(chatId, messageId);
                    return editMessageText;
                case "myWorkTimeStatusApprove":
                    long between = ChronoUnit.DAYS.between(holidayState.startDate, holidayState.endDate);
                    if (between > 0) {
                        LocalDate date = holidayState.startDate;
                        for (int i = 0; i <= between; i++) {
                            Appointment appointment = new Appointment();
                            appointment.setAppointmentDate(date);
                            appointment.setStatus(AppointmentStatus.BANNED);
                            appointment.setAppointmentTime(WorkTime.EIGHT);
                            appointment.setMaster(masterService.findByChatId(chatId));
                            appointment.setWorkStatus(holidayState.getWorkStatus());
                            appointment.setService(serviceService.findByName("holiday"));

                            date = date.plusDays(1);
                            appointmentService.create(appointment);
                        }

                        holidayState = new HolidayState();
                    }
                    editMessageText = menu(chatId, messageId);
                    return editMessageText;
                case "wayToSalon":
                    sendLocation = sendSalonLocation(chatId);
                    return sendLocation;
                case "reauthorize":
                    editMessageText = start(chatId, messageId);
                    return editMessageText;
                default:
                    break;
            }
        }

        return sendMessage;
    }

    private EditMessageText managementWorkTime(Long chatId, Long messageId) {
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(chatId);
        editMessageText.setMessageId(messageId.intValue());
        StringBuilder text = new StringBuilder();

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        if (!holidayState.checkWorkStatus()) {
            text.append("Выберите тип выходного:").append("\n\n");

            for (WorkStatus status : WorkStatus.values()) {
                if (status != WorkStatus.WORKING) {
                    List<InlineKeyboardButton> row = new ArrayList<>();
                    InlineKeyboardButton button = new InlineKeyboardButton();
                    button.setText(status.getDescription());
                    button.setCallbackData("myWorkTimeStatus_" + status.name());
                    row.add(button);
                    keyboard.add(row);
                }
            }

            addMainMenuButton(keyboard);

            markup.setKeyboard(keyboard);
            editMessageText.setReplyMarkup(markup);

            editMessageText.setText(convertToEmoji(text.toString()));
            return editMessageText;
        }

        if (!holidayState.checkStartDate()) {
            text.append("Выберите дату начала выходного:").append("\n")
                    .append("(Если Вы хотите сделать 1 выходной, то выбранные Вами даты начала и конца должны совпадать)").append("\n\n");

            editMessageText = selectDateByCalendar("myWorkTimeStartDate", "myWorkTimeStartDatePrev", "myWorkTimeStartDateNext", chatId, messageId);
            editMessageText.setText(convertToEmoji(text.toString()));
            return editMessageText;
        }

        if (!holidayState.checkEndDate()) {
            text.append("Выберите дату окончания выходного:").append("\n\n");

            editMessageText = selectDateByCalendar("myWorkTimeEndDate", "myWorkTimeEndDatePrev", "myWorkTimeEndDateNext", chatId, messageId);
            editMessageText.setText(convertToEmoji(text.toString()));
            return editMessageText;
        }

        if (!holidayState.checkApprove()) {
            text.append("Подтвердите Ваш выбор:").append("\n")
                    .append("Статус: ").append(holidayState.workStatus.getDescription()).append("\n")
                    .append("Дата начала: ").append(holidayState.startDate).append("\n")
                    .append("Дата конца: ").append(holidayState.endDate).append("\n\n");


            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(convertToEmoji("Подтверждаю"));
            button.setCallbackData("myWorkTimeStatusApprove_");
            row.add(button);
            keyboard.add(row);

            List<InlineKeyboardButton> row1 = new ArrayList<>();
            InlineKeyboardButton button2 = new InlineKeyboardButton();
            button2.setText(convertToEmoji(":x: Отмена"));
            button2.setCallbackData("menu");
            row1.add(button2);
            keyboard.add(row1);

            addMainMenuButton(keyboard);

            markup.setKeyboard(keyboard);
            editMessageText.setReplyMarkup(markup);

            editMessageText.setText(convertToEmoji(text.toString()));
            return editMessageText;
        }

        return null;
    }

    private EditMessageText cancelService(Long chatId, Long messageId, Appointment appointmentSelectedServiceRateCancel) {
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(chatId);
        editMessageText.setMessageId(messageId.intValue());
        StringBuilder text = new StringBuilder();
        text.append("Подтвердите отмену процедуры:").append("\n\n");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton button1 = new InlineKeyboardButton();
        button1.setText(convertToEmoji(":white_check_mark: Подтверждаю"));
        button1.setCallbackData("myServicesSelectRateCancelServiceApprove_" + appointmentSelectedServiceRateCancel.getId());
        row1.add(button1);
        keyboard.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton button2 = new InlineKeyboardButton();
        button2.setText(convertToEmoji(":x: Отмена"));
        button2.setCallbackData("menu");
        row2.add(button2);
        keyboard.add(row2);

        addMainMenuButton(keyboard);

        markup.setKeyboard(keyboard);
        editMessageText.setReplyMarkup(markup);

        String s = EmojiParser.parseToUnicode(text.toString());
        editMessageText.setText(s);
        return editMessageText;
    }

    private EditMessageText endService(Long chatId, Long messageId, Appointment appointmentSelectedServiceRateEnd) {
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(chatId);
        editMessageText.setMessageId(messageId.intValue());
        StringBuilder text = new StringBuilder();
        text.append("Подтвердите завершение процедуры:").append("\n\n");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton button1 = new InlineKeyboardButton();
        button1.setText(convertToEmoji(":white_check_mark: Подтверждаю"));
        button1.setCallbackData("myServicesSelectRateEndServiceApprove_" + appointmentSelectedServiceRateEnd.getId());
        row1.add(button1);
        keyboard.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton button2 = new InlineKeyboardButton();
        button2.setText(convertToEmoji(":x: Отмена"));
        button2.setCallbackData("menu");
        row2.add(button2);
        keyboard.add(row2);

        addMainMenuButton(keyboard);

        markup.setKeyboard(keyboard);
        editMessageText.setReplyMarkup(markup);

        String s = EmojiParser.parseToUnicode(text.toString());
        editMessageText.setText(s);
        return editMessageText;
    }

    private EditMessageText menuSelectRate(Long chatId, Long messageId) {
        EditMessageText sendMessage = new EditMessageText();
        sendMessage.setChatId(chatId);
        sendMessage.setMessageId(messageId.intValue());
        StringBuilder text = new StringBuilder();
        text.append("Выберите действие с текущей записью:").append("\n\n");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        Appointment appointment = rateState.getAppointment();

        if (appointment.getWorkStatus() == null) {
            text.append(":elf:").append("Клиент: ").append(appointment.getClient().getName()).append("\n")
                    .append(":telephone: ").append("Номер клиента: ").append(appointment.getClient().getPhoneNumber()).append("\n")
                    .append(":cherry_blossom: ").append("Вид услуги:: ").append(appointment.getService().getKind()).append("\n")
                    .append(":bell: ").append("Услуга: ").append(appointment.getService().getName()).append("\n")
                    .append(":calendar: ").append("Дата: ").append(appointment.getAppointmentDate()).append("\n")
                    .append(":mantelpiece_clock: ").append("Время: ").append(appointment.getAppointmentTime().getDescription()).append("\n")
                    .append(":hourglass: ").append("Продолжительность: " + convertMinutesToHours(appointment.getService().getDuration())).append("\n")
                    .append(":money_with_wings: ").append("Цена: " + appointment.getService().getPrice()).append(" nis\n\n");
        } else {
            text.append("У Вас сегодня '").append(appointment.getWorkStatus().getDescription()).append("'\n");
        }

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton button1 = new InlineKeyboardButton();
        button1.setText(convertToEmoji(":white_check_mark: Завершить услугу"));
        button1.setCallbackData("myServicesSelectRateEndService_" + appointment.getId());
        row1.add(button1);
        keyboard.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton button2 = new InlineKeyboardButton();
        button2.setText(convertToEmoji(":x: Отменить услугу"));
        button2.setCallbackData("myServicesSelectRateCancelService_" + appointment.getId());
        row2.add(button2);
        keyboard.add(row2);

        addMainMenuButton(keyboard);

        markup.setKeyboard(keyboard);
        sendMessage.setReplyMarkup(markup);

        String s = EmojiParser.parseToUnicode(text.toString());
        sendMessage.setText(s);
        return sendMessage;
    }

    private EditMessageText selectServicePeriod(Long chatId, Long messageId) {
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId);
        message.setMessageId(messageId.intValue());
        message.setText(convertToEmoji("Выберите когда хотите посмотреть записи:\n"));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton button1 = new InlineKeyboardButton();
        button1.setText(convertToEmoji("На сегодня"));
        button1.setCallbackData("myServicesToday");
        row1.add(button1);
        keyboard.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton button2 = new InlineKeyboardButton();
        button2.setText(convertToEmoji("На завтра"));
        button2.setCallbackData("myServicesTomorrow");
        row2.add(button2);
        keyboard.add(row2);

        List<InlineKeyboardButton> row4 = new ArrayList<>();
        InlineKeyboardButton button4 = new InlineKeyboardButton();
        button4.setText(convertToEmoji("Выбрать дату"));
        button4.setCallbackData("myServicesSelectDate");
        row4.add(button4);
        keyboard.add(row4);

        addMainMenuButton(keyboard);

        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);

        return message;
    }

    private SendMessage start(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        StringBuilder text = new StringBuilder();

        if (masterService.existsByChatId(chatId)) {
            text.append(":fireworks: Вы уже авторизованы! Можете пользоваться ботом!:fireworks:");

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

            addMainMenuButton(keyboard);

            markup.setKeyboard(keyboard);
            sendMessage.setReplyMarkup(markup);
        } else {
            text.append(":tada: Добро пожаловать в чат-бот для мастеров!:tada:\n")
                    .append(":closed_lock_with_key: Введите пожалуйста ваши логин и пароль через пробел (login password) :closed_lock_with_key:");
        }

        String s = EmojiParser.parseToUnicode(text.toString());
        sendMessage.setText(s);

        return sendMessage;
    }

    private EditMessageText start(Long chatId, Long messageId) {
        EditMessageText sendMessage = new EditMessageText();
        sendMessage.setChatId(chatId);
        sendMessage.setMessageId(messageId.intValue());
        StringBuilder text = new StringBuilder();

        if (masterService.existsByChatId(chatId)) {
            text.append(":fireworks: Вы уже авторизованы! Можете пользоваться ботом!:fireworks:");

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

            addMainMenuButton(keyboard);

            markup.setKeyboard(keyboard);
            sendMessage.setReplyMarkup(markup);
        } else {
            text.append(":tada: Добро пожаловать в чат-бот для мастеров!:tada:\n")
                    .append(":closed_lock_with_key: Введите пожалуйста ваши логин и пароль через пробел (login password) :closed_lock_with_key:");
        }

        String s = EmojiParser.parseToUnicode(text.toString());
        sendMessage.setText(s);

        return sendMessage;
    }

    private EditMessageText showBookedServices(Long chatId, Long messageId, LocalDate date) {
        EditMessageText sendMessage = new EditMessageText();
        sendMessage.setChatId(chatId);
        sendMessage.setMessageId(messageId.intValue());
        StringBuilder text = new StringBuilder();
        text.append(":calendar:").append("Забронированные услуги:").append("\n\n");

        Master master = masterService.findByChatId(chatId);
        List<Appointment> appointmentsByMaster = appointmentService.getAppointmentsByDateAndMaster(date, master);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        int counter = 0;
        List<InlineKeyboardButton> row = new ArrayList<>();

        for (Appointment appointment : appointmentsByMaster) {
            if (appointment.getStatus().equals(AppointmentStatus.BANNED)) {
                text.append(":hash: ").append("Номер ").append(counter+1).append(":\n")
                        .append(":elf:").append("Клиент: ").append(appointment.getClient().getName()).append("\n")
                        .append(":telephone: ").append("Номер клиента: ").append(appointment.getClient().getPhoneNumber()).append("\n")
                        .append(":cherry_blossom: ").append("Вид услуги:: ").append(appointment.getService().getKind()).append("\n")
                        .append(":bell: ").append("Услуга: ").append(appointment.getService().getName()).append("\n")
                        .append(":calendar: ").append("Дата: ").append(appointment.getAppointmentDate()).append("\n")
                        .append(":mantelpiece_clock: ").append("Время: ").append(appointment.getAppointmentTime().getDescription()).append("\n")
                        .append(":hourglass: ").append("Продолжительность: " + convertMinutesToHours(appointment.getService().getDuration())).append("\n")
                        .append(":money_with_wings: ").append("Цена: " + appointment.getService().getPrice()).append(" nis\n\n");

                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(convertToEmoji(":hash: Услуга " + (counter + 1)));
                button.setCallbackData("myServicesSelectRate_" + appointment.getId());
                row.add(button);

                if (row.size() == 2) {
                    keyboard.add(row);
                    row = new ArrayList<>();
                }

                counter++;
            }
        }

        if (!row.isEmpty()) {
            keyboard.add(row);
        }

        addMainMenuButton(keyboard);

        markup.setKeyboard(keyboard);
        sendMessage.setReplyMarkup(markup);

        String s = EmojiParser.parseToUnicode(text.toString());
        sendMessage.setText(s);
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
                addMainMenuButton(keyboard);
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
        message.setText(convertToEmoji(":point_down: Выберите действие из меню :point_down:"));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton button1 = new InlineKeyboardButton();
        button1.setText(convertToEmoji(":mag_right: Посмотреть записи"));
        button1.setCallbackData("myServices");
        row1.add(button1);
        keyboard.add(row1);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton button3 = new InlineKeyboardButton();
        button3.setText(convertToEmoji(":wrench: Управление рабочим временем"));
        button3.setCallbackData("myWorkTime");
        row3.add(button3);
        keyboard.add(row3);

        List<InlineKeyboardButton> row4 = new ArrayList<>();
        InlineKeyboardButton button4 = new InlineKeyboardButton();
        button4.setText(convertToEmoji(":oncoming_taxi: Добраться до салона"));
        button4.setCallbackData("wayToSalon");
        row4.add(button4);
        keyboard.add(row4);


        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);

        return message;
    }

    private EditMessageText menu(Long chatId, Long messageId) {
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId);
        message.setMessageId(messageId.intValue());
        message.setText(convertToEmoji("Выберите действие из меню:\n"));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton button1 = new InlineKeyboardButton();
        button1.setText(convertToEmoji(":mag_right: Посмотреть записи"));
        button1.setCallbackData("myServices");
        row1.add(button1);
        keyboard.add(row1);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton button3 = new InlineKeyboardButton();
        button3.setText(convertToEmoji(":wrench: Управление рабочим временем"));
        button3.setCallbackData("myWorkTime");
        row3.add(button3);
        keyboard.add(row3);

        List<InlineKeyboardButton> row4 = new ArrayList<>();
        InlineKeyboardButton button4 = new InlineKeyboardButton();
        button4.setText(convertToEmoji(":oncoming_taxi: Добраться до салона"));
        button4.setCallbackData("wayToSalon");
        row4.add(button4);
        keyboard.add(row4);

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

            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(convertToEmoji(":house_with_garden: Главное меню :house_with_garden:"));
            button.setCallbackData("menuSendMessage");
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            keyboard.add(row);

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
        text.append(":white_check_mark:").append("У вас новая запись!").append(":white_check_mark:").append("\n")
                .append(":elf:").append("Клиент: ").append(client.getName()).append("\n")
                .append(":bell:").append("Услуга: ").append(service.getName()).append("\n")
                .append(":calendar:").append("Дата: ").append(appointment.getAppointmentDate()).append("\n")
                .append(":mantelpiece_clock:").append("Время: ").append(appointment.getAppointmentTime().getDescription());

        String emojiString = EmojiParser.parseToUnicode(text.toString());
        sendMessage.setText(emojiString);

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

        text.append(":x:").append("У вас отмена записи!").append(":x:").append("\n")
                .append(":elf:").append("Клиент: ").append(client.getName()).append("\n")
                .append(":bell:").append("Услуга: ").append(service.getName()).append("\n")
                .append(":calendar:").append("Дата: ").append(appointment.getAppointmentDate()).append("\n")
                .append(":mantelpiece_clock:").append("Время: ").append(appointment.getAppointmentTime().getDescription());

        String emojiString = EmojiParser.parseToUnicode(text.toString());
        sendMessage.setText(emojiString);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Some error with answer:" + e.getMessage());
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

    private String getDataCallbackQuery(String data, int x) {
        String[] parts = data.split("_");
        String address = parts[x];
        return address;
    }

    private String convertToEmoji(String text) {
        String s = EmojiParser.parseToUnicode(text);
        return s;
    }

    private String splitLoginAndPassword(String data, int x) {
        String[] parts = data.split(" ");
        String address = parts[x];
        return address;
    }

    private boolean checkLoginAndPasswordStructure(String data) {
        String[] parts = data.split(" ");
        return parts.length == 2;
    }

    private EditMessageText selectDateByCalendar(String name, String prev, String next, Long chatId, Long messageId) {
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(chatId);
        editMessageText.setMessageId(messageId.intValue());

        editMessageText.setText(convertToEmoji("Выберите дату:\n"));
        List<LocalDate> allDates = getAvailableDates(400);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        InlineKeyboardMarkup calendarMarkup = createCalendar(masterService.findByChatId(chatId), name, prev, next, calendarState.getSelectMonth(), allDates);

        editMessageText.setReplyMarkup(calendarMarkup);
        markup.setKeyboard(keyboard);

        return editMessageText;
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

    public InlineKeyboardMarkup createCalendar(Master master, String name, String prev, String next, LocalDate currentDate, List<LocalDate> availableDates) {
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
                                dayButton.setCallbackData("date_" + currentDateInLoop.toString());
                            } else if (appointmentsByDateAndMaster.get(0).getWorkStatus() == WorkStatus.VACATION) {
                                dayButton.setText(convertToEmoji(String.valueOf(dayCounter) + ":airplane:"));
                                dayButton.setCallbackData("date_" + currentDateInLoop.toString());
                            } else if (appointmentsByDateAndMaster.get(0).getWorkStatus() == WorkStatus.DAY_OFF) {
                                dayButton.setText(convertToEmoji(String.valueOf(dayCounter) + ":palm_tree:"));
                                dayButton.setCallbackData("date_" + currentDateInLoop.toString());
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
        prevButton.setCallbackData(prev);
        navRow.add(prevButton);
        InlineKeyboardButton menuButton = new InlineKeyboardButton();
        menuButton.setText(convertToEmoji(":house_with_garden: Меню"));
        menuButton.setCallbackData("menu_");
        navRow.add(menuButton);
        InlineKeyboardButton nextButton = new InlineKeyboardButton();
        nextButton.setText(convertToEmoji(":arrow_forward:"));
        nextButton.setCallbackData(next);
        navRow.add(nextButton);
        keyboard.add(navRow);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
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
}
