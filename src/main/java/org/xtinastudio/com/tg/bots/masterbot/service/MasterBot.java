package org.xtinastudio.com.tg.bots.masterbot.service;

import com.mysql.cj.protocol.a.LocalDateValueEncoder;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendLocation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.xtinastudio.com.entity.*;
import org.xtinastudio.com.enums.*;
import org.xtinastudio.com.service.*;
import org.xtinastudio.com.tg.bots.clientbot.service.ClientNotice;
import org.xtinastudio.com.tg.bots.clientbot.service.RatingState;
import org.xtinastudio.com.tg.bots.masterbot.service.states.MasterReportState;
import org.xtinastudio.com.tg.properties.MasterBotProperties;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

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
    private ClientNotice clientNotice;

    @Autowired
    private ClientService clientService;

    @Autowired
    private  MasterReviewService masterReviewService;

    @Autowired
    private ClientReviewsService clientReviewsService;

    @Autowired
    private ServiceService serviceService;

    RatingState ratingStateForClient = new RatingState();

    DeleteState deleteState = new DeleteState();

    CalendarState calendarState = new CalendarState();

    RateState rateState = new RateState();

    HolidayState holidayState = new HolidayState();

    MasterReportState masterReportState = new MasterReportState();

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
                    deleteMessageById(chatId.toString(), messageId.intValue());
                    masterReportState = new MasterReportState();
                    holidayState = new HolidayState();
                    sendMessage = menu(chatId);
                    return sendMessage;
                case "menu":
                    masterReportState = new MasterReportState();
                    holidayState = new HolidayState();
                    editMessageText = menu(chatId, messageId);
                    return editMessageText;
                case "myProfile":
                    editMessageText = myProfile(chatId, messageId);
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
                case "myServicesSelectRateForClientApprove":
                    int myServicesSelectRateForClientData = Integer.parseInt(getDataCallbackQuery(data, 1));
                    ratingStateForClient.setMasterRating(myServicesSelectRateForClientData);
                    editMessageText = endService(chatId, messageId, ratingStateForClient.getAppointment());
                    return editMessageText;
                case "myServicesSelectRateEndService":
                    String selectedServiceRateEndService = getDataCallbackQuery(data, 1);
                    Appointment appointmentSelectedServiceRateEnd = appointmentService.getById(Long.parseLong(selectedServiceRateEndService));
                    editMessageText = sendCheckRating(chatId, messageId, appointmentSelectedServiceRateEnd, "myServicesSelectRateForClientApprove_", "buttonNext", "buttonPrev");
                    return editMessageText;
                case "myServicesSelectRateCancelService":
                    String selectedServiceRateCancelService = getDataCallbackQuery(data, 1);
                    Appointment appointmentSelectedServiceRateCancel = appointmentService.getById(Long.parseLong(selectedServiceRateCancelService));
                    editMessageText = sendCheckRating(chatId, messageId, appointmentSelectedServiceRateCancel, "myServicesSelectRateForClientCancel_", "buttonNext", "buttonPrev");
                    return editMessageText;
                case "myServicesSelectRateForClientCancel":
                    int myServicesSelectRateForClientCancelData = Integer.parseInt(getDataCallbackQuery(data, 1));
                    ratingStateForClient.setMasterRating(myServicesSelectRateForClientCancelData);
                    editMessageText = cancelService(chatId, messageId, ratingStateForClient.getAppointment());
                    return editMessageText;
                case "myServicesSelectRateEndServiceApprove":
                    String appointmentId = getDataCallbackQuery(data, 1);
                    Appointment approve = appointmentService.getById(Long.parseLong(appointmentId));
                    approve.setStatus(AppointmentStatus.COMPLETED);
                    appointmentService.editById(approve.getId(), approve);
                    ClientReview clientReview = new ClientReview();
                    clientReview.setClient(ratingStateForClient.getAppointment().getClient());
                    clientReview.setMaster(ratingStateForClient.getAppointment().getMaster());
                    clientReview.setRating(ratingStateForClient.getMasterRating());
                    clientReview.setReviewDate(LocalDateTime.now());
                    clientReviewsService.create(clientReview);
                    clientNotice.sendCheckRatingToClient(approve);
                    editMessageText = menu(chatId, messageId);
                    return editMessageText;
                case "myServicesSelectRateCancelServiceApprove":
                    String dataCallbackQuery = getDataCallbackQuery(data, 1);
                    Appointment cancel = appointmentService.getById(Long.parseLong(dataCallbackQuery));
                    cancel.setStatus(AppointmentStatus.CANCELED);
                    appointmentService.editById(cancel.getId(), cancel);
                    ClientReview clientReview2 = new ClientReview();
                    clientReview2.setClient(ratingStateForClient.getAppointment().getClient());
                    clientReview2.setMaster(ratingStateForClient.getAppointment().getMaster());
                    clientReview2.setRating(ratingStateForClient.getMasterRating());
                    clientReview2.setReviewDate(LocalDateTime.now());
                    clientReviewsService.create(clientReview2);
                    clientNotice.sendCancelMessageToClient(cancel);
                    editMessageText = menu(chatId, messageId);
                    return editMessageText;
                case "myWorkTime":
                    editMessageText = menuSelectCreateDeleteWorkTime(chatId, messageId);
                    return editMessageText;
                case "myWorkTimeDelete":
                    editMessageText = selectDateDeleteVacation(chatId, messageId);
                    return editMessageText;
                case "myWorkTimeDeleteApprove":
                    deleteState.setStartDate(LocalDate.parse(getDataCallbackQuery(data, 1)));
                    System.out.println(deleteState.getStartDate());
                    deleteState.setEndDate(LocalDate.parse(getDataCallbackQuery(data, 2)));
                    System.out.println(deleteState.getEndDate());
                    deleteState.setWorkStatus(WorkStatus.valueOf(getDataCallbackQuery(data, 3)));
                    System.out.println(deleteState.getWorkStatus());
                    editMessageText = approveDateDeleteVacation(chatId, messageId);
                    return editMessageText;
                case "myWorkTimeDeleteApproveAction":
                    Master master = masterService.findByChatId(chatId);
                    deleteVacation(master, deleteState.getStartDate(), deleteState.getEndDate());
                    editMessageText = menu(chatId, messageId);
                    return editMessageText;
                case "myWorkTimeCreate":
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
                    long between = ChronoUnit.DAYS.between(holidayState.getStartDate(), holidayState.getEndDate());

                    if (between >= 0) {
                        LocalDate date = holidayState.startDate;
                        for (int i = 0; i <= between; i++) {
                            Appointment appointment = new Appointment();
                            appointment.setAppointmentDate(date);
                            appointment.setStatus(AppointmentStatus.BANNED);
                            appointment.setAppointmentTime(WorkTime.EIGHT);
                            appointment.setMaster(masterService.findByChatId(chatId));
                            appointment.setWorkStatus(holidayState.getWorkStatus());
                            appointment.setService(serviceService.findByName("holiday"));

                            if (between > 0) {
                                date = date.plusDays(1);
                            }

                            appointmentService.create(appointment);
                        }

                        holidayState = new HolidayState();
                    }
                    editMessageText = menu(chatId, messageId);
                    return editMessageText;
                case "wayToSalon":
                    deleteMessageById(chatId.toString(), messageId.intValue());
                    sendLocation = sendSalonLocation(chatId);
                    return sendLocation;
                case "reauthorize":
                    editMessageText = start(chatId, messageId);
                    return editMessageText;
                case "masterReportsSelectSalon":
                    editMessageText = masterReport(chatId, messageId);
                    return editMessageText;
                case "masterReportsSelectMaster":
                    String masterReportsSelectMaster_Data = getDataCallbackQuery(data, 1);
                    long masterReportsSelectMaster_SalonId = Long.parseLong(masterReportsSelectMaster_Data);
                    masterReportState.setSalon(salonService.findById(masterReportsSelectMaster_SalonId));
                    editMessageText = masterReport(chatId, messageId);
                    return editMessageText;
                case "masterReportsSelectTimePeriod":
                    String masterReportsSelectTimePeriod_Data = getDataCallbackQuery(data, 1);
                    long masterReportsSelectTimePeriod_MasterId = Long.parseLong(masterReportsSelectTimePeriod_Data);
                    masterReportState.setMaster(masterService.findById(masterReportsSelectTimePeriod_MasterId));
                    editMessageText = masterReport(chatId, messageId);
                    return editMessageText;
                case "masterReportSendReport":
                    String masterReportSendReport_Data = getDataCallbackQuery(data, 1);
                    masterReportState.setReportTimePeriod(ReportTimePeriod.valueOf(masterReportSendReport_Data));
                    editMessageText = masterReport(chatId, messageId);
                    return editMessageText;
                default:
                    break;
            }
        }

        return sendMessage;
    }

    public EditMessageText masterReport(Long chatId, Long messageId) {
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId);
        message.setMessageId(messageId.intValue());

        StringBuilder text = new StringBuilder();

        List<Salon> salons = salonService.getAll();

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        if (!masterReportState.checkSalon()) {
            text.append("Выберете салон:\n\n");

            for (Salon salon : salons) {
                List<InlineKeyboardButton> row = new ArrayList<>();
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(convertToEmoji(":office: " + salon.getAddress()));
                button.setCallbackData("masterReportsSelectMaster_" + salon.getId());
                row.add(button);
                keyboard.add(row);
            }
            addMainMenuButton(keyboard);

            markup.setKeyboard(keyboard);
            message.setReplyMarkup(markup);
            message.setText(convertToEmoji(text.toString()));

            return message;
        }

        if (!masterReportState.checkMaster()) {
            text.append("Выберете мастера:\n\n");

            List<Master> masters = masterService.getAllBySalon(masterReportState.getSalon());

            for (Master master : masters) {
                List<InlineKeyboardButton> row = new ArrayList<>();
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(convertToEmoji(":star: " + master.getName()));
                button.setCallbackData("masterReportsSelectTimePeriod_" + master.getId());
                row.add(button);
                keyboard.add(row);
            }
            addMainMenuButton(keyboard);

            markup.setKeyboard(keyboard);
            message.setReplyMarkup(markup);
            message.setText(convertToEmoji(text.toString()));

            return message;
        }

        if (!masterReportState.checkReportTimePeriod()) {
            text.append("Выберете отчёт за последний:\n\n");

            for (ReportTimePeriod period : ReportTimePeriod.values()) {
                List<InlineKeyboardButton> row = new ArrayList<>();
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(convertToEmoji(":scroll: " + period.getDescription()));
                button.setCallbackData("masterReportSendReport_" + period.name());
                row.add(button);
                keyboard.add(row);
            }
            addMainMenuButton(keyboard);

            markup.setKeyboard(keyboard);
            message.setReplyMarkup(markup);
            message.setText(convertToEmoji(text.toString()));

            return message;
        }

        if (!masterReportState.checkSendReport()) {
            deleteMessageById(chatId.toString(), messageId.intValue());
            Master master = masterReportState.getMaster();
            ReportTimePeriod reportTimePeriod = masterReportState.getReportTimePeriod();

            List<Appointment> appointments;
            if (reportTimePeriod == ReportTimePeriod.MONTH) {
                appointments = appointmentService.getAppointmentsForLastDays(master, 30);
            } else if (reportTimePeriod == ReportTimePeriod.YEAR) {
                appointments = appointmentService.getAppointmentsForLastDays(master, 365);
            } else {
                appointments = appointmentService.getAppointmentsByMaster(master);
            }

            text.append("Отчёт за последний ").append(reportTimePeriod.getDescription()).append(":\n\n");

            int totalAppointments = 0;
            int totalDuration = 0;
            BigDecimal totalPrice = BigDecimal.ZERO;

            int completedCount = 0;
            int canceledCount = 0;

            for (Appointment appointment : appointments) {
                if (appointment.getWorkStatus() == null) {
                    int duration;

                    if (appointment.getDuration() != null) {
                        duration = appointment.getDuration();
                    } else {
                        duration = appointment.getService().getDuration();
                    }

                    text.append("Дата: ").append(appointment.getAppointmentDate()).append("\n")
                            .append("Клиент: ").append(appointment.getClient().getName()).append("\n")
                            .append("Услуга: ").append(appointment.getService().getName()).append("\n")
                            .append("Статус: ").append(appointment.getStatus()).append("\n")
                            .append("Длительность: ").append(duration).append(" минут").append("\n")
                            .append("Цена: ").append(appointment.getService().getPrice()).append(" руб").append("\n\n");

                    if (appointment.getStatus().equals(AppointmentStatus.COMPLETED)) {
                        totalPrice = totalPrice.add(appointment.getService().getPrice());

                        if (appointment.getDuration() != null) {
                            totalDuration += appointment.getDuration();
                        } else {
                            totalDuration += appointment.getService().getDuration();
                        }
                    }

                    if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
                        completedCount++;
                    } else if (appointment.getStatus() == AppointmentStatus.CANCELED) {
                        canceledCount++;
                    }
                }
            }

            totalAppointments = completedCount + canceledCount;

            text.append("Итоги:\n")
                    .append("Всего записей: ").append(totalAppointments).append("\n")
                    .append("Общая длительность: ").append(totalDuration).append(" минут").append("\n")
                    .append("Общая стоимость: ").append(totalPrice).append(" руб").append("\n")
                    .append("Завершено: ").append(completedCount).append("\n")
                    .append("Отменено: ").append(canceledCount).append("\n");

            markup.setKeyboard(keyboard);
            message.setReplyMarkup(markup);
            message.setText(convertToEmoji(text.toString()));

            Path tempFilePath = null;
            try {
                tempFilePath = Files.createTempFile(master.getName() + " " + masterReportState.getReportTimePeriod().getDescription(), ".txt");
                Files.write(tempFilePath, text.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (tempFilePath != null) {
                List<InlineKeyboardButton> row = new ArrayList<>();
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(convertToEmoji(":house_with_garden: Главное меню :house_with_garden:"));
                button.setCallbackData("menuSendMessage");
                row.add(button);
                keyboard.add(row);

                markup.setKeyboard(keyboard);

                SendDocument sendDocument = new SendDocument();
                sendDocument.setChatId(chatId);
                sendDocument.setReplyMarkup(markup);
                sendDocument.setCaption("Отчёт по мастеру за " + masterReportState.getReportTimePeriod().getDescription());
                try {
                    sendDocument.setDocument(new InputFile(new ByteArrayInputStream(Files.readAllBytes(tempFilePath)), master.getName() + " " + masterReportState.getReportTimePeriod().getDescription() + ".txt"));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                try {
                    execute(sendDocument);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }

                try {
                    Files.deleteIfExists(tempFilePath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }

        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);
        message.setText(convertToEmoji(text.toString()));

        return message;
    }


    public EditMessageText sendCheckRating(Long chatId, Long messageId, Appointment appointment, String buttonData, String buttonNext, String buttonPrev) {
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(chatId);
        editMessageText.setMessageId(messageId.intValue());
        StringBuilder text = new StringBuilder();

        Master master = appointment.getMaster();
        Services service = appointment.getService();
        Client client = appointment.getClient();

        ratingStateForClient.setAppointment(appointment);
        ratingStateForClient.setMaster(master);

        text.append(":bulb: Поставьте оценку колиенту '").append(client.getName())
                .append("' побывавшего у Вас на процедуре '").append(service.getName()).append("':\n");

        text.append("\nВы можете не ставить оценку клиенту нажав далее меню.\n\n");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(convertToEmoji(String.valueOf(i) + " :star:"));
            button.setCallbackData(buttonData + i);
            if (i <= 3) {
                row1.add(button);
            } else {
                row2.add(button);
            }
        }

        keyboard.add(row1);
        keyboard.add(row2);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(convertToEmoji(convertToEmoji(":arrow_left: Назад ")));
        button.setCallbackData("next");

        InlineKeyboardButton button2 = new InlineKeyboardButton();
        button2.setText(convertToEmoji(convertToEmoji(":arrow_right: Далее ")));
        button2.setCallbackData("next");
        keyboard.add(row3);

        addMainMenuButton(keyboard);

        markup.setKeyboard(keyboard);
        editMessageText.setReplyMarkup(markup);

        editMessageText.setChatId(client.getChatId());
        editMessageText.setText(convertToEmoji(text.toString()));

        return editMessageText;
    }

    private void deleteVacation(Master master, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date must not be null");
        }

        long between = ChronoUnit.DAYS.between(startDate, endDate);
        LocalDate date = startDate;

        if (between >= 0) {
            for (int i = 0; i <= between; i++) {
                List<Appointment> appointmentsByDateAndMaster = appointmentService.getAppointmentsByDateAndMaster(date, master);

                if (!appointmentsByDateAndMaster.isEmpty()) {
                    for (Appointment appointment : appointmentsByDateAndMaster) {
                        appointment.setWorkStatus(null);
                        appointment.setStatus(AppointmentStatus.CANCELED);
                        appointmentService.editById(appointment.getId(), appointment);
                    }
                }

                date = date.plusDays(1);
            }
        }
    }

    private EditMessageText approveDateDeleteVacation(Long chatId, Long messageId) {
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(chatId);
        editMessageText.setMessageId(messageId.intValue());
        StringBuilder text = new StringBuilder();

        text.append("Подтвердите, что Вы хотите удалить данный выходной:").append("\n\n");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(":white_check_mark: Подтверждаю");
        button.setCallbackData("myWorkTimeDeleteApproveAction_");
        row.add(button);
        keyboard.add(row);

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton button2 = new InlineKeyboardButton();
        button2.setText(":arrow_left: Назад");
        button2.setCallbackData("myWorkTimeDelete_");
        row1.add(button2);
        keyboard.add(row1);

        addMainMenuButton(keyboard);

        markup.setKeyboard(keyboard);
        editMessageText.setReplyMarkup(markup);

        editMessageText.setText(convertToEmoji(text.toString()));
        return editMessageText;
    }

    private EditMessageText selectDateDeleteVacation(Long chatId, Long messageId) {
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(chatId);
        editMessageText.setMessageId(messageId.intValue());
        StringBuilder text = new StringBuilder();

        text.append("Выберите выходной который хотите удалить:").append("\n\n");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<Appointment> appointments = appointmentService.getAll().stream()
                .filter(appointment -> appointment.getWorkStatus() != null)
                .sorted(Comparator.comparing(Appointment::getAppointmentDate))
                .collect(Collectors.toList());

        if (!appointments.isEmpty()) {
            LocalDate startDate = appointments.get(0).getAppointmentDate();
            LocalDate endDate = startDate;
            WorkStatus currentStatus = appointments.get(0).getWorkStatus();

            for (int i = 1; i < appointments.size(); i++) {
                Appointment appointment = appointments.get(i);
                LocalDate date = appointment.getAppointmentDate();
                WorkStatus status = appointment.getWorkStatus();

                if (status == currentStatus && date.equals(endDate.plusDays(1))) {
                    endDate = date;
                } else {
                    addDateRangeButton(keyboard, startDate, endDate, currentStatus);
                    startDate = date;
                    endDate = date;
                    currentStatus = status;
                }
            }

            addDateRangeButton(keyboard, startDate, endDate, currentStatus);
        }

        addMainMenuButton(keyboard);

        markup.setKeyboard(keyboard);
        editMessageText.setReplyMarkup(markup);

        editMessageText.setText(convertToEmoji(text.toString()));
        return editMessageText;
    }

    private void addDateRangeButton(List<List<InlineKeyboardButton>> keyboard, LocalDate startDate, LocalDate endDate, WorkStatus status) {
        String dateRangeText = startDate.equals(endDate)
                ? startDate.toString()
                : startDate.toString() + " - " + endDate.toString();

        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(convertToEmoji(":calendar: " + dateRangeText + " (" + status.getDescription() + ")"));
        button.setCallbackData("myWorkTimeDeleteApprove_" + startDate + "_" + endDate + "_" + status);

        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(button);
        keyboard.add(row);
    }

    private EditMessageText menuSelectCreateDeleteWorkTime(Long chatId, Long messageId) {
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(chatId);
        editMessageText.setMessageId(messageId.intValue());
        StringBuilder text = new StringBuilder();

        text.append("Если Вы хотите добавить выходной/больничный/отпуск в свой календарь, то выбирите 'Добавить выходной'.").append("\n")
                .append("Если наоброт удалить, то - 'Удалить выходной'.").append("\n\n");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(convertToEmoji(":white_check_mark: Добавить выходной"));
        button.setCallbackData("myWorkTimeCreate_");
        row.add(button);
        keyboard.add(row);

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton button1 = new InlineKeyboardButton();
        button1.setText(convertToEmoji(":x: Удалить выходной"));
        button1.setCallbackData("myWorkTimeDelete_");
        row1.add(button1);
        keyboard.add(row1);

        addMainMenuButton(keyboard);

        markup.setKeyboard(keyboard);
        editMessageText.setReplyMarkup(markup);

        editMessageText.setText(convertToEmoji(text.toString()));
        return editMessageText;
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
                    if (status.equals(WorkStatus.SICK)) {
                        button.setText(convertToEmoji(":hospital: " + status.getDescription()));
                    } else if (status.equals(WorkStatus.DAYOFF)) {
                        button.setText(convertToEmoji(":palm_tree: " + status.getDescription()));
                    } else if (status.equals(WorkStatus.VACATION)) {
                        button.setText(convertToEmoji(":airplane: " + status.getDescription()));
                    }
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
                    .append(":bell: Статус: ").append(holidayState.workStatus.getDescription()).append("\n")
                    .append(":calendar: Дата начала: ").append(holidayState.startDate).append("\n")
                    .append(":calendar: Дата конца: ").append(holidayState.endDate).append("\n\n");


            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(convertToEmoji(":white_check_mark: Подтверждаю"));
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

        text.append(":elf:").append("Клиент: ").append(appointment.getClient().getName()).append("\n")
                .append(":telephone: ").append("Номер клиента: ").append(appointment.getClient().getPhoneNumber()).append("\n")
                .append(":cherry_blossom: ").append("Вид услуги:: ").append(appointment.getService().getKind()).append("\n")
                .append(":bell: ").append("Услуга: ").append(appointment.getService().getName()).append("\n")
                .append(":calendar: ").append("Дата: ").append(appointment.getAppointmentDate()).append("\n")
                .append(":mantelpiece_clock: ").append("Время: ").append(appointment.getAppointmentTime().getDescription()).append("\n")
                .append(":hourglass: ").append("Продолжительность: " + convertMinutesToHours(appointment.getService().getDuration())).append("\n")
                .append(":money_with_wings: ").append("Цена: " + appointment.getService().getPrice()).append(" nis\n\n");

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
        button1.setText(convertToEmoji(":arrow_down: На сегодня"));
        button1.setCallbackData("myServicesToday");
        row1.add(button1);
        keyboard.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton button2 = new InlineKeyboardButton();
        button2.setText(convertToEmoji(":arrow_heading_down: На завтра"));
        button2.setCallbackData("myServicesTomorrow");
        row2.add(button2);
        keyboard.add(row2);

        List<InlineKeyboardButton> row4 = new ArrayList<>();
        InlineKeyboardButton button4 = new InlineKeyboardButton();
        button4.setText(convertToEmoji(":calendar: Выбрать дату"));
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
                int duration;
                if (appointment.getDuration() != null) {
                    duration = appointment.getDuration();
                } else {
                    duration = appointment.getService().getDuration();
                }

                text.append(":hash: ").append("Номер ").append(counter+1).append(":\n")
                        .append(":elf: ").append("Клиент: ").append(appointment.getClient().getName()).append("\n");

                Double clientRating = clientReviewsService.getClientRating(appointment.getClient());
                if (clientRating != null) {
                    text.append(":star: ").append("Рейтинг: ").append(roundToTwoDecimalPlaces(clientRating)).append("\n");
                } else {
                    text.append(":star: ").append("Рейтинг: ").append("рейтинг отсутствует").append("\n");
                }

                text.append(":telephone: ").append("Номер клиента: ").append(appointment.getClient().getPhoneNumber()).append("\n")
                        .append(":cherry_blossom: ").append("Вид услуги:: ").append(appointment.getService().getKind()).append("\n")
                        .append(":bell: ").append("Услуга: ").append(appointment.getService().getName()).append("\n")
                        .append(":calendar: ").append("Дата: ").append(appointment.getAppointmentDate()).append("\n")
                        .append(":mantelpiece_clock: ").append("Время: ").append(appointment.getAppointmentTime().getDescription()).append("\n")
                        .append(":hourglass: ").append("Продолжительность: " + duration).append("\n")
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

        List<InlineKeyboardButton> row5 = new ArrayList<>();
        InlineKeyboardButton button5 = new InlineKeyboardButton();
        button5.setText(convertToEmoji(":open_book: Мой профиль"));
        button5.setCallbackData("myProfile");
        row5.add(button5);
        keyboard.add(row5);

        if (masterService.findByChatId(chatId).getRole() != Role.MASTER) {
            List<InlineKeyboardButton> row6 = new ArrayList<>();
            InlineKeyboardButton button6 = new InlineKeyboardButton();
            button6.setText(convertToEmoji(":scroll: Посмотреть отчёты"));
            button6.setCallbackData("masterReportsSelectSalon");
            row6.add(button6);
            keyboard.add(row6);
        }

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

        List<InlineKeyboardButton> row5 = new ArrayList<>();
        InlineKeyboardButton button5 = new InlineKeyboardButton();
        button5.setText(convertToEmoji(":open_book: Мой профиль"));
        button5.setCallbackData("myProfile");
        row5.add(button5);
        keyboard.add(row5);

        if (masterService.findByChatId(chatId).getRole() != Role.MASTER) {
            List<InlineKeyboardButton> row6 = new ArrayList<>();
            InlineKeyboardButton button6 = new InlineKeyboardButton();
            button6.setText(convertToEmoji(":scroll: Посмотреть отчёты"));
            button6.setCallbackData("masterReportsSelectSalon");
            row6.add(button6);
            keyboard.add(row6);
        }

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

    public EditMessageText myProfile(Long chatId, Long messageId) {
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId);
        message.setMessageId(messageId.intValue());
        StringBuilder text = new StringBuilder();

        Master master = masterService.findByChatId(chatId);

        text.append("Ваш профиль:\n\n")
                .append(":elf:").append("Имя: ").append(master.getName()).append("\n")
                .append(":office:").append("Салон: ").append(master.getSalon().getAddress()).append("\n")
                .append(":star:").append("Рейтинг: ").append(masterReviewService.getMasterRating(master)).append("\n")
                .append(":link:").append("Ваш url: ").append(master.getUrl()).append("\n");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        addMainMenuButton(keyboard);

        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);
        message.setText(convertToEmoji(text.toString()));

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
        Master master = masterService.findByChatId(chatId);

        editMessageText.setText(convertToEmoji("Выберите дату:\n"));
        List<LocalDate> allDates = getAvailableDates(400);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        InlineKeyboardMarkup calendarMarkup = createCalendar(master, name, prev, next, calendarState.getSelectMonth(), allDates);

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
                        dayButton.setCallbackData(name + "_" + currentDateInLoop.toString());

                        if (!appointmentsByDateAndMaster.isEmpty()) {
                            if (appointmentsByDateAndMaster.get(0).getWorkStatus() == WorkStatus.SICK) {
                                dayButton.setText(convertToEmoji(String.valueOf(dayCounter) + ":hospital:"));
                                dayButton.setCallbackData(name + "_" + currentDateInLoop.toString());
                            } else if (appointmentsByDateAndMaster.get(0).getWorkStatus() == WorkStatus.VACATION) {
                                dayButton.setText(convertToEmoji(String.valueOf(dayCounter) + ":airplane:"));
                                dayButton.setCallbackData(name + "_" + currentDateInLoop.toString());
                            } else if (appointmentsByDateAndMaster.get(0).getWorkStatus() == WorkStatus.DAYOFF) {
                                dayButton.setText(convertToEmoji(String.valueOf(dayCounter) + ":palm_tree:"));
                                dayButton.setCallbackData(name + "_" + currentDateInLoop.toString());
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

    public static double roundToTwoDecimalPlaces(double value) {
        BigDecimal bigDecimal = BigDecimal.valueOf(value);
        bigDecimal = bigDecimal.setScale(2, RoundingMode.HALF_UP);
        return bigDecimal.doubleValue();
    }

    public void deleteMessageById(String chatId, int messageId) {
        DeleteMessage deleteMessage = new DeleteMessage(chatId, messageId);

        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            log.error("Some error with answer:" + e.getMessage());
        }
    }
}
