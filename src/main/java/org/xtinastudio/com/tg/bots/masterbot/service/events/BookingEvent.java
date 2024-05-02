package org.xtinastudio.com.tg.bots.masterbot.service.events;

import org.springframework.context.ApplicationEvent;
import org.xtinastudio.com.entity.Appointment;

public class BookingEvent extends ApplicationEvent {

    private final Appointment appointment;

    public BookingEvent(Object source, Appointment appointment) {
        super(source);
        this.appointment = appointment;
    }

    public Appointment getAppointment() {
        return appointment;
    }
}
