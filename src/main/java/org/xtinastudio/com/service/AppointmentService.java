package org.xtinastudio.com.service;

import org.xtinastudio.com.entity.Appointment;

import java.util.List;

public interface AppointmentService {

    Appointment create(Appointment appointment);

    Appointment editById(Long id, Appointment appointment);

    Appointment findAppointmentByDate(Long id);

    List<Appointment> getAll();

    void delete(Long id);
}
