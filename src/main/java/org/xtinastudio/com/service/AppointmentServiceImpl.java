package org.xtinastudio.com.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xtinastudio.com.entity.Appointment;
import org.xtinastudio.com.entity.Client;
import org.xtinastudio.com.entity.Master;
import org.xtinastudio.com.entity.Services;
import org.xtinastudio.com.exceptions.AppointmentNotFoundException;
import org.xtinastudio.com.repository.AppointmentJpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppointmentServiceImpl implements AppointmentService {

    @Autowired
    AppointmentJpaRepository repository;

    @Override
    public Appointment create(Appointment appointment) {
        return repository.save(appointment);
    }

    @Override
    public Appointment editById(Long id, Appointment appointment) {
        Appointment existingAppointment = repository.findById(id).orElseThrow(() -> new AppointmentNotFoundException("Appointment not found with id: " + id));

        existingAppointment.setAppointmentDate(appointment.getAppointmentDate());
        existingAppointment.setAppointmentTime(appointment.getAppointmentTime());
        existingAppointment.setService(appointment.getService());
        existingAppointment.setMaster(appointment.getMaster());
        existingAppointment.setClient(appointment.getClient());
        existingAppointment.setStatus(appointment.getStatus());

        Appointment updatedAppointment = repository.save(existingAppointment);

        return updatedAppointment;
    }

    @Override
    public List<Appointment> getAppointmentsByDateAndServiceAndMaster(LocalDate date, Master master) {
        return repository.getAppointmentsByDateAndServiceAndMaster(date, master);
    }

    @Override
    public List<Appointment> getAppointmentsByClient(Client client) {
        return repository.getAppointmentsByClient(client);
    }

    @Override
    public List<Appointment> getAll() {
        return repository.findAll();
    }

    @Override
    public void deleteById(Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
        } else {
            throw new AppointmentNotFoundException("Appointment not found with id: " + id);
        }
    }
}
