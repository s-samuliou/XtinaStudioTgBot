package org.xtinastudio.com.tg.bots.masterbot.service;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class CalendarState {

    LocalDate selectDate;

    LocalDate selectMonth = LocalDate.now();

    boolean checkDate() {
        if (this.selectDate == null) {
            return false;
        }
        return true;
    }
}
