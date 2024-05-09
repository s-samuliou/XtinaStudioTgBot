package org.xtinastudio.com.service;

import org.xtinastudio.com.entity.Master;
import org.xtinastudio.com.entity.Salon;
import org.xtinastudio.com.entity.Services;

import java.util.List;

public interface MasterService {

    Master create(Master master);

    Master editById(Long id, Master master);

    Master findById(Long id);

    Master findByLogin(String login);

    Master findByName(String name);

    Master findByChatId(Long chatId);

    List<Master> findByServicesContainingAndSalon(Services services, Salon salon);

    boolean existsByChatId(Long chatId);

    List<Master> getAll();

    List<Master> getAllBySalon(Salon salon);

    void delete(Long id);
}
