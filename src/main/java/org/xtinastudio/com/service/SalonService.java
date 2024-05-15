package org.xtinastudio.com.service;

import org.xtinastudio.com.entity.Salon;
import org.xtinastudio.com.entity.Services;

import java.util.List;

public interface SalonService {

    Salon create(Salon salonInfo);

    Salon editById(Long id, Salon salonInfo);

    Salon findById(Long id);

    Salon findByName(String name);

    List<Salon> getAll();

    void delete(Long id);
}
