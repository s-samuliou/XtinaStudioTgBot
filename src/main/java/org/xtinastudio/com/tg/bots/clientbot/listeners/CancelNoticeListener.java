package org.xtinastudio.com.tg.bots.clientbot.listeners;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.xtinastudio.com.tg.bots.clientbot.events.CancelNoticeEvent;
import org.xtinastudio.com.tg.bots.clientbot.service.ClientBot;

@Component
public class CancelNoticeListener {

    private final ClientBot clientBot;

    public CancelNoticeListener(ClientBot clientBot) {
        this.clientBot = clientBot;
    }

    @EventListener
    public void handleCancelEvent(CancelNoticeEvent event) {
        clientBot.sendCancelNoticeToClient(event.getAppointment());
    }
}
