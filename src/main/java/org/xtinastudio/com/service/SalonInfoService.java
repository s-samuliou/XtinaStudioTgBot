package org.xtinastudio.com.service;

import org.xtinastudio.com.entity.SalonInfo;

import java.util.List;

public interface SalonInfoService {

    SalonInfo create(SalonInfo salonInfo);

    SalonInfo editById(Long id, SalonInfo salonInfo);

    SalonInfo findById(Long id);

    SalonInfo findByName(String name);

    List<SalonInfo> getAll();

    void delete(Long id);
}
