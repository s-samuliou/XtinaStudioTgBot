package org.xtinastudio.com.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xtinastudio.com.entity.SalonInfo;
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
        return null;
    }

    @Override
    public SalonInfo findSalonInfoById(Long id) {
        return null;
    }

    @Override
    public List<SalonInfo> getAll() {
        return repository.findAll();
    }

    @Override
    public void delete(Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
        }
    }
}
