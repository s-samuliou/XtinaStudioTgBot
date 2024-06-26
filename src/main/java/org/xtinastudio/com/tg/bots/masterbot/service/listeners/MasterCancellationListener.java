package org.xtinastudio.com.tg.bots.masterbot.service.listeners;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.xtinastudio.com.tg.bots.masterbot.service.MasterBot;
import org.xtinastudio.com.tg.bots.masterbot.service.events.CancellationEvent;

@Component
public class MasterCancellationListener {

    private final MasterBot messageMasterService;

    public MasterCancellationListener(MasterBot messageMasterService) {
        this.messageMasterService = messageMasterService;
    }

    @EventListener
    public void handleCancellationEvent(CancellationEvent event) {
        messageMasterService.sendCanceledNoticeToMaster(event.getAppointment());
    }
}
