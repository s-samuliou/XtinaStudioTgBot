package org.xtinastudio.com.tg.bots.masterbot.service.events;

import org.springframework.context.ApplicationEvent;
import org.xtinastudio.com.entity.Appointment;

public class CancellationEvent extends ApplicationEvent {

    private final Appointment appointment;

    public CancellationEvent(Object source, Appointment appointment) {
        super(source);
        this.appointment = appointment;
    }

    public Appointment getAppointment() {
        return appointment;
    }
}

