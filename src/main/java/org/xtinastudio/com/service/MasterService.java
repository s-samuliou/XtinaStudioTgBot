package org.xtinastudio.com.service;

import org.xtinastudio.com.entity.Master;

import java.util.List;

public interface MasterService {

    Master create(Master master);

    Master editById(Long id, Master master);

    Master findById(Long id);

    Master findByLogin(String login);

    Master findByName(String name);

    List<Master> getAll();

    void delete(Long id);
}
