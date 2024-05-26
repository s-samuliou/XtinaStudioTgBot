package org.xtinastudio.com.service;

import org.xtinastudio.com.entity.Master;
import org.xtinastudio.com.entity.Salon;
import org.xtinastudio.com.entity.Services;

import java.util.List;

public interface ServiceService {

    Services create(Services service);

    Services editById(Long id, Services service);

    Services findById(Long id);

    Services findByName(String name);

    List<Services> findBySalons(Salon salon);

    List<Services> findBySalonsAndKind(Salon salon, String kind);

    List<Services> getAll();

    void delete(Long id);
}
