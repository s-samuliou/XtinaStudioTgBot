package org.xtinastudio.com.tg.bots.clientbot.listeners;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.xtinastudio.com.tg.bots.clientbot.events.RateMasterEvent;
import org.xtinastudio.com.tg.bots.clientbot.service.ClientBot;

@Component
public class RateMasterListener {
    private final ClientBot clientBot;

    public RateMasterListener(ClientBot clientBot) {
        this.clientBot = clientBot;
    }

    @EventListener
    public void handleRatingEvent(RateMasterEvent event) {
        clientBot.sendCheckRating(event.getAppointment());
    }
}
