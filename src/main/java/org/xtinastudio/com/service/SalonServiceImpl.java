package org.xtinastudio.com.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xtinastudio.com.entity.Salon;
import org.xtinastudio.com.exceptions.SalonInfoNotFoundException;
import org.xtinastudio.com.repository.SalonJpaRepository;

import java.util.List;

@Service
public class SalonServiceImpl implements SalonService {

    @Autowired
    SalonJpaRepository repository;

    @Override
    public Salon create(Salon salonInfo) {
        return repository.save(salonInfo);
    }

    @Override
    public Salon editById(Long id, Salon salonInfo) {
        Salon existingSalonInfo = repository.findById(id).orElseThrow(() -> new SalonInfoNotFoundException("Salon info not found with id: " + id));

        existingSalonInfo.setLatitude(salonInfo.getLatitude());
        existingSalonInfo.setLongitude(salonInfo.getLongitude());

        Salon updatedSalonInfo = repository.save(existingSalonInfo);
        return updatedSalonInfo;
    }

    @Override
    public Salon findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new SalonInfoNotFoundException("Salon info not found with id: " + id));
    }

    @Override
    public Salon findByName(String name) {
        return repository.findByAddress(name);
    }

    @Override
    public List<Salon> getAll() {
        return repository.findAll();
    }

    @Override
    public void delete(Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
        } else {
            throw new SalonInfoNotFoundException("Salon info not found with id: " + id);
        }
    }
}
