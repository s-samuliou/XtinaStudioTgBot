package org.xtinastudio.com.service;

import org.xtinastudio.com.entity.Services;

import java.util.List;

public interface ServiceService {

    Services create(Services service);

    Services editById(Long id, Services service);

    Services findById(Long id);

    Services findByName(String name);

    List<Services> getAll();

    void delete(Long id);
}
