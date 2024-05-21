package org.xtinastudio.com.tg.bots.clientbot.service;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.xtinastudio.com.entity.Appointment;
import org.xtinastudio.com.entity.Master;

@Data
@NoArgsConstructor
public class RatingState {

    Appointment appointment;

    Master master;

    int masterRating;
}
