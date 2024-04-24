package org.xtinastudio.com.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.xtinastudio.com.entity.Appointment;
import org.xtinastudio.com.entity.Client;
import org.xtinastudio.com.entity.Master;
import org.xtinastudio.com.entity.Services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentJpaRepository extends JpaRepository<Appointment, Long> {

    @Query("SELECT a FROM Appointment a " +
            "WHERE a.appointmentDate = :date " +
            "AND a.master = :master")
    List<Appointment> getAppointmentsByDateAndServiceAndMaster(@Param("date") LocalDate date,
                                                               @Param("master") Master master);

    @Query("SELECT a FROM Appointment a WHERE a.client = :client")
    List<Appointment> getAppointmentsByClient(@Param("client") Client client);
}
