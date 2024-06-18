package org.xtinastudio.com.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;
import org.xtinastudio.com.dto.MasterCreateDto;
import org.xtinastudio.com.dto.MasterDto;
import org.xtinastudio.com.entity.Master;

@Mapper(componentModel = "spring")
public interface MasterMapper {

    MasterMapper INSTANCE = Mappers.getMapper(MasterMapper.class);

    @Mappings({
            @Mapping(source = "id", target = "id"),
            @Mapping(source = "chatId", target = "chatId"),
            @Mapping(source = "name", target = "name"),
            @Mapping(source = "login", target = "login"),
            @Mapping(source = "password", target = "password"),
            @Mapping(source = "url", target = "url"),
            @Mapping(source = "description", target = "description"),
            @Mapping(source = "role", target = "role"),
            @Mapping(source = "workStatus", target = "workStatus"),
            @Mapping(source = "salon.id", target = "salonId") // assuming salon has an id field
    })
    MasterCreateDto masterToMasterCreateDto(Master master);

    @Mappings({
            @Mapping(target = "appointments", ignore = true),
            @Mapping(target = "masterReviews", ignore = true),
            @Mapping(target = "services", ignore = true),
            @Mapping(target = "photoUrl", ignore = true),
            @Mapping(target = "salon", ignore = true)
    })
    Master masterCreateDtoToMaster(MasterCreateDto masterCreateDto);

    MasterDto masterToMasterDto(Master master);

    Master masterDtoToMaster(MasterDto masterDto);
}
