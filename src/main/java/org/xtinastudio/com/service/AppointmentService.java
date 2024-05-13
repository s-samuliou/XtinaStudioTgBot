package org.xtinastudio.com.service;

import org.xtinastudio.com.entity.Appointment;
import org.xtinastudio.com.entity.Client;
import org.xtinastudio.com.entity.Master;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentService {

    Appointment create(Appointment appointment);

    Appointment editById(Long id, Appointment appointment);

    List<Appointment> getAppointmentsByDateAndMaster(LocalDate date, Master master);

    List<Appointment> getAppointmentsByClient(Client client);

    List<Appointment> getAppointmentsByMaster(Master master);

    Appointment getById(Long id);

    List<Appointment> getAll();

    void deleteById(Long id);
}
