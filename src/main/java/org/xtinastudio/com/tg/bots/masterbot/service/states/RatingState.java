package org.xtinastudio.com.tg.bots.masterbot.service.states;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.xtinastudio.com.entity.Appointment;
import org.xtinastudio.com.entity.Master;

@Data
@NoArgsConstructor
public class RatingState {

    Appointment appointment;

    Master master;

    int clientRating;
}
