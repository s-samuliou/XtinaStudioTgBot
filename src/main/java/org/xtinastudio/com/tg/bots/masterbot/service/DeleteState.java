package org.xtinastudio.com.tg.bots.masterbot.service;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.xtinastudio.com.enums.WorkStatus;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class DeleteState {

    LocalDate startDate;

    LocalDate endDate;

    WorkStatus workStatus;

    boolean checkStartDate() {
        if (this.startDate == null) {
            return false;
        }
        return true;
    }

    boolean checkEndDate() {
        if (this.endDate == null) {
            return false;
        }
        return true;
    }

    boolean checkWorkStatus() {
        if (this.workStatus == null) {
            return false;
        }
        return true;
    }
}
