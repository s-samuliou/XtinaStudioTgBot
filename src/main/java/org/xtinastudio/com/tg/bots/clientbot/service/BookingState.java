package org.xtinastudio.com.tg.bots.clientbot.service;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.xtinastudio.com.entity.Client;
import org.xtinastudio.com.entity.Master;
import org.xtinastudio.com.entity.Services;
import org.xtinastudio.com.enums.WorkTime;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class BookingState {

    String serviceKind;

    Services service;

    Master master;

    LocalDate date;

    WorkTime workTime;

    LocalDate selectMonth = LocalDate.now();

    Boolean individualTime;

    Integer duration;

    boolean checkDuration() {
        if (this.duration == null) {
            return false;
        }
        return true;
    }

    boolean checkIndividualTime() {
        if (this.individualTime == null) {
            return false;
        }
        return true;
    }

    boolean checkSelectMonth() {
        if (this.selectMonth == null) {
            return false;
        }
        return true;
    }

    boolean checkServiceKind() {
        if (this.serviceKind == null) {
            return false;
        }
        return true;
    }

    boolean checkService() {
        if (this.service == null) {
            return false;
        }
        return true;
    }

    boolean checkMaster() {
        if (this.master == null) {
            return false;
        }
        return true;
    }

    boolean checkDate() {
        if (this.date == null) {
            return false;
        }
        return true;
    }

    boolean checkTime() {
        if (this.workTime == null) {
            return false;
        }
        return true;
    }
}
