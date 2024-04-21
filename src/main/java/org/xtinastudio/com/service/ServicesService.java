package org.xtinastudio.com.service;

import org.xtinastudio.com.entity.Service;

import java.util.List;

public interface ServicesService {

    Service create(Service service);

    Service editById(Long id, Service service);

    Service findById(Long id);

    List<Service> getAll();

    void delete(Long id);
}
