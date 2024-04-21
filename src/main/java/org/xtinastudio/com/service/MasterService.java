package org.xtinastudio.com.service;

import org.xtinastudio.com.entity.Master;

import java.util.List;

public interface MasterService {

    Master create(Master master);

    Master editById(Long id, Master master);

    Master findMasterById(Long id);

    List<Master> getAll();

    void delete(Long id);
}
