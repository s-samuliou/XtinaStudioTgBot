package org.xtinastudio.com.tg.bots.clientbot.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.xtinastudio.com.entity.Appointment;
import org.xtinastudio.com.tg.bots.clientbot.events.CancelNoticeEvent;
import org.xtinastudio.com.tg.bots.clientbot.events.RateMasterEvent;

@Service
public class ClientNotice {
    private final ApplicationEventPublisher eventPublisher;

    public ClientNotice(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void sendCheckRatingToClient(Appointment appointment) {
        RateMasterEvent rateMasterEvent = new RateMasterEvent(this, appointment);
        eventPublisher.publishEvent(rateMasterEvent);
    }

    public void sendCancelMessageToClient(Appointment appointment) {
        CancelNoticeEvent cancelNoticeEvent = new CancelNoticeEvent(this, appointment);
        eventPublisher.publishEvent(cancelNoticeEvent);
    }
}
