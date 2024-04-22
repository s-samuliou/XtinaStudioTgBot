package org.xtinastudio.com.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.xtinastudio.com.entity.Appointment;
import org.xtinastudio.com.entity.Master;
import org.xtinastudio.com.entity.Services;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentJpaRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> getAppointmentByServiceAndMasterAndAppointmentDate(Services service, Master master, String appointmentDate);
}
