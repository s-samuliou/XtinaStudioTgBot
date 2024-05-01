package org.xtinastudio.com.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.xtinastudio.com.entity.Salon;
import org.xtinastudio.com.entity.Services;
import org.xtinastudio.com.repository.ServiceJpaRepository;

import java.util.List;

@org.springframework.stereotype.Service
public class ServiceServiceImpl implements ServiceService {

    @Autowired
    ServiceJpaRepository repository;

    @Override
    public Services create(Services service) {
        return repository.save(service);
    }

    @Override
    public Services editById(Long id, Services service) {
        return null;
    }

    @Override
    public Services findById(Long id) {
        return repository.findById(id).orElseThrow();
    }

    @Override
    public Services findByName(String name) {
        return repository.findByName(name);
    }

    @Override
    public List<Services> findBySalons(Salon salon) {
        return repository.findBySalons(salon);
    }

    @Override
    public List<Services> getAll() {
        return repository.findAll();
    }

    @Override
    public void delete(Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
        }
    }
}
