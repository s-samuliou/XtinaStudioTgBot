package org.xtinastudio.com.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.xtinastudio.com.entity.Service;
import org.xtinastudio.com.repository.ServicesJpaRepository;

import java.util.List;

@org.springframework.stereotype.Service
public class ServicesServiceImpl implements ServicesService {

    @Autowired
    ServicesJpaRepository repository;

    @Override
    public Service create(Service service) {
        return repository.save(service);
    }

    @Override
    public Service editById(Long id, Service service) {
        return null;
    }

    @Override
    public Service findById(Long id) {
        return repository.findById(id).orElseThrow();
    }

    @Override
    public List<Service> getAll() {
        return repository.findAll();
    }

    @Override
    public void delete(Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
        }
    }
}
