package org.xtinastudio.com.tg.bots.clientbot.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.xtinastudio.com.entity.Appointment;
import org.xtinastudio.com.tg.bots.clientbot.events.RateMasterEvent;
import org.xtinastudio.com.tg.bots.masterbot.service.events.BookingEvent;

@Service
public class MasterRatingNotice {
    private final ApplicationEventPublisher eventPublisher;

    public MasterRatingNotice(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void sendCheckRatingToClient(Appointment appointment) {
        RateMasterEvent rateMasterEvent = new RateMasterEvent(this, appointment);
        eventPublisher.publishEvent(rateMasterEvent);
    }
}
