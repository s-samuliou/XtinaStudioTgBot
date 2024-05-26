package org.xtinastudio.com.tg.bots.masterbot.service.states;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.xtinastudio.com.entity.Master;
import org.xtinastudio.com.entity.Salon;
import org.xtinastudio.com.enums.ReportTimePeriod;

@Data
@NoArgsConstructor
public class MasterReportState {

    Salon salon;

    Master master;

    ReportTimePeriod reportTimePeriod;

    Boolean sendReport = false;

    public boolean checkSalon() {
        if (this.salon == null) {
            return false;
        }
        return true;
    }

    public boolean checkMaster() {
        if (this.master == null) {
            return false;
        }
        return true;
    }

    public boolean checkReportTimePeriod() {
        if (this.reportTimePeriod == null) {
            return false;
        }
        return true;
    }

    public boolean checkSendReport() {
        if (!this.sendReport) {
            return false;
        }
        return true;
    }
}
