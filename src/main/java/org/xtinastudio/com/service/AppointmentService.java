package org.xtinastudio.com.service;

import org.springframework.data.repository.query.Param;
import org.xtinastudio.com.entity.Appointment;
import org.xtinastudio.com.entity.Client;
import org.xtinastudio.com.entity.Master;
import org.xtinastudio.com.entity.Services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentService {

    Appointment create(Appointment appointment);

    Appointment editById(Long id, Appointment appointment);

    List<Appointment> getAppointmentsByDateAndServiceAndMaster(LocalDate date, Master master);

    List<Appointment> getAppointmentsByClient(Client client);

    List<Appointment> getAll();

    void deleteById(Long id);
}
