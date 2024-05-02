package org.xtinastudio.com.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xtinastudio.com.entity.Master;
import org.xtinastudio.com.entity.Salon;
import org.xtinastudio.com.entity.Services;
import org.xtinastudio.com.exceptions.MasterNotFoundException;
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
        Master existingMaster = repository.findById(id).orElseThrow(() -> new MasterNotFoundException("Master not found with id: " + id));

        existingMaster.setName(master.getName());
        existingMaster.setChatId(master.getChatId());
        existingMaster.setLastName(master.getLastName());
        existingMaster.setDescription(master.getDescription());
        existingMaster.setPhotoUrl(master.getPhotoUrl());
        existingMaster.setRole(master.getRole());
        existingMaster.setWorkStatus(master.getWorkStatus());
        existingMaster.setLogin(master.getLogin());
        existingMaster.setPassword(master.getPassword());

        Master updatedMaster = repository.save(existingMaster);

        return updatedMaster;
    }

    @Override
    public Master findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new MasterNotFoundException("Master not found with id: " + id));
    }

    @Override
    public Master findByLogin(String login) {
        return repository.findByLogin(login);
    }

    @Override
    public Master findByName(String name) {
        return findByName(name);
    }

    @Override
    public Master findByChatId(Long chatId) {
        return repository.findByChatId(chatId);
    }

    @Override
    public List<Master> findByServicesContainingAndSalon(Services services, Salon salon) {
        return repository.findByServicesContainingAndSalon(services, salon);
    }

    @Override
    public boolean existsByChatId(Long chatId) {
        return repository.existsByChatId(chatId);
    }


    @Override
    public List<Master> getAll() {
        return repository.findAll();
    }

    @Override
    public void delete(Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
        } else {
            throw new MasterNotFoundException("Master not found with id: " + id);
        }
    }
}
