package org.xtinastudio.com.tg.bots.clientbot.events;

import org.springframework.context.ApplicationEvent;

public class RateMasterEvent extends ApplicationEvent {
    public RateMasterEvent(Object source) {
        super(source);
    }
}
