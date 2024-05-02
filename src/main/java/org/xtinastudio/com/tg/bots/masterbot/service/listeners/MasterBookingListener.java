package org.xtinastudio.com.tg.bots.masterbot.service.listeners;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.xtinastudio.com.tg.bots.masterbot.service.MessageMasterService;
import org.xtinastudio.com.tg.bots.masterbot.service.events.BookingEvent;

@Component
public class MasterBookingListener {
    private final MessageMasterService messageMasterService;

    public MasterBookingListener(MessageMasterService messageMasterService) {
        this.messageMasterService = messageMasterService;
    }

    @EventListener
    public void handleBookingEvent(BookingEvent event) {
        messageMasterService.sendBookedNoticeToMaster(event.getAppointment());
    }
}
