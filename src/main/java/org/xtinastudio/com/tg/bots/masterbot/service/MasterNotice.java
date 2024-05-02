package org.xtinastudio.com.tg.bots.masterbot.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.xtinastudio.com.entity.Appointment;
import org.xtinastudio.com.tg.bots.masterbot.service.events.BookingEvent;
import org.xtinastudio.com.tg.bots.masterbot.service.events.CancellationEvent;

@Service
public class MasterNotice {

    private final ApplicationEventPublisher eventPublisher;

    public MasterNotice(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void sendBookedNoticeToMaster(Appointment appointment) {
        BookingEvent bookingEvent = new BookingEvent(this, appointment);
        eventPublisher.publishEvent(bookingEvent);
    }

    public void sendCanceledNoticeToMaster(Appointment appointment) {
        CancellationEvent cancellationEvent = new CancellationEvent(this, appointment);
        eventPublisher.publishEvent(cancellationEvent);
    }
}
