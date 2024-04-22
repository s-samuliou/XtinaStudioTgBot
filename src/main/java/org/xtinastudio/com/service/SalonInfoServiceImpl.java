package org.xtinastudio.com.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xtinastudio.com.entity.SalonInfo;
import org.xtinastudio.com.exceptions.SalonInfoNotFoundException;
import org.xtinastudio.com.repository.SalonInfoJpaRepository;

import java.util.List;

@Service
public class SalonInfoServiceImpl implements SalonInfoService {

    @Autowired
    SalonInfoJpaRepository repository;

    @Override
    public SalonInfo create(SalonInfo salonInfo) {
        return repository.save(salonInfo);
    }

    @Override
    public SalonInfo editById(Long id, SalonInfo salonInfo) {
        SalonInfo existingSalonInfo = repository.findById(id).orElseThrow(() -> new SalonInfoNotFoundException("Salon info not found with id: " + id));

        existingSalonInfo.setLatitude(salonInfo.getLatitude());
        existingSalonInfo.setLongitude(salonInfo.getLongitude());

        SalonInfo updatedSalonInfo = repository.save(existingSalonInfo);
        return updatedSalonInfo;
    }

    @Override
    public SalonInfo findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new SalonInfoNotFoundException("Salon info not found with id: " + id));
    }

    @Override
    public SalonInfo findByName(String name) {
        return repository.findByName(name);
    }

    @Override
    public List<SalonInfo> getAll() {
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
