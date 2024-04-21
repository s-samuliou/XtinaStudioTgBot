package org.xtinastudio.com.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xtinastudio.com.entity.Master;
import org.xtinastudio.com.repository.MasterJpaRepository;

import java.util.List;

@Service
public class MasterServiceImpl implements MasterService {

    @Autowired
    MasterJpaRepository repository;

    @Override
    public Master create(Master master) {
        return repository.save(master);
    }

    @Override
    public Master editById(Long id, Master master) {
        return null;
    }

    @Override
    public Master findMasterById(Long id) {
        return null;
    }

    @Override
    public List<Master> getAll() {
        return repository.findAll();
    }

    @Override
    public void delete(Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
        }
    }
}
