package org.xtinastudio.com.converter;

import org.mapstruct.Mapper;
import org.xtinastudio.com.dto.MasterCreateDto;
import org.xtinastudio.com.dto.MasterDto;
import org.xtinastudio.com.entity.Master;

@Mapper(componentModel = "spring")
public interface MasterMapper {

    MasterCreateDto masterToMasterCreateDto(Master master);

    Master masterCreateDtoToMaster(MasterCreateDto masterCreateDto);

    MasterDto masterToMasterDto(Master master);

    Master masterDtoToMaster(MasterDto masterDto);
}
