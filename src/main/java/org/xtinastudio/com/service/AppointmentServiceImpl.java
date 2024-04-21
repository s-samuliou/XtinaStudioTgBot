package org.xtinastudio.com.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xtinastudio.com.entity.Appointment;
import org.xtinastudio.com.repository.AppointmentJpaRepository;

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
        return null;
    }

    @Override
    public Appointment findAppointmentByDate(Long id) {
        return null;
    }

    @Override
    public List<Appointment> getAll() {
        return repository.findAll();
    }

    @Override
    public void delete(Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
        }
    }
}
