package org.xtinastudio.com.tg.bots.clientbot.events;

import org.springframework.context.ApplicationEvent;
import org.xtinastudio.com.entity.Appointment;

public class CancelNoticeEvent extends ApplicationEvent {

    private final Appointment appointment;

    public CancelNoticeEvent(Object source, Appointment appointment) {
        super(source);
        this.appointment = appointment;
    }

    public Appointment getAppointment() {
        return appointment;
    }
}
