package org.xtinastudio.com.service;

import org.xtinastudio.com.entity.SalonInfo;

import java.util.List;

public interface SalonInfoService {

    SalonInfo create(SalonInfo salonInfo);

    SalonInfo editById(Long id, SalonInfo salonInfo);

    SalonInfo findSalonInfoById(Long id);

    List<SalonInfo> getAll();

    void delete(Long id);
}
