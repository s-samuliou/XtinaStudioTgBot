package org.xtinastudio.com.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xtinastudio.com.converter.MasterMapper;
import org.xtinastudio.com.dto.MasterCreateDto;
import org.xtinastudio.com.entity.Master;
import org.xtinastudio.com.exceptions.MasterInvalidArgumentException;
import org.xtinastudio.com.exceptions.MasterNotFoundException;
import org.xtinastudio.com.security.AuthenticationService;
import org.xtinastudio.com.service.MasterService;

@Slf4j
@Tag(name="Services Controller", description="Handles operations related to services")
@RestController
@RequestMapping("/v1/services")
public class ServicesController {

    @Autowired
    MasterService masterService;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private MasterMapper mapper;

    @Operation(
            summary = "Create a new service",
            description = "Creates a new service based on the provided data",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Successfully created service"),
                    @ApiResponse(responseCode = "400", description = "Bad request"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @PostMapping()
    public ResponseEntity<MasterCreateDto> create(@RequestBody MasterCreateDto masterCreateDto) {
        log.info("Received request to create master: {}", masterCreateDto);
        Master master = mapper.masterCreateDtoToMaster(masterCreateDto);
        Master createdMaster = masterService.create(master);
        MasterCreateDto createdMasterDto = mapper.masterToMasterCreateDto(createdMaster);
        log.debug("Master created: {}", createdMasterDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(masterCreateDto);
    }

    @Operation(
            summary = "Get all services",
            description = "Gets all services",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Successfully gets service"),
                    @ApiResponse(responseCode = "400", description = "Bad request"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @GetMapping()
    public ResponseEntity<MasterCreateDto> getAllMasters(@RequestBody MasterCreateDto masterCreateDto) {
        log.info("Received request to create master: {}", masterCreateDto);
        Master master = mapper.masterCreateDtoToMaster(masterCreateDto);
        Master createdMaster = masterService.create(master);
        MasterCreateDto createdMasterDto = mapper.masterToMasterCreateDto(createdMaster);
        log.debug("Master created: {}", createdMasterDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(masterCreateDto);
    }

    @Operation(
            summary = "Update a service",
            description = "Update a service",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Successfully updated service"),
                    @ApiResponse(responseCode = "400", description = "Bad request"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @PutMapping()
    public ResponseEntity<MasterCreateDto> update(@RequestBody MasterCreateDto masterCreateDto) {
        log.info("Received request to create master: {}", masterCreateDto);
        Master master = mapper.masterCreateDtoToMaster(masterCreateDto);
        Master createdMaster = masterService.create(master);
        MasterCreateDto createdMasterDto = mapper.masterToMasterCreateDto(createdMaster);
        log.debug("Master created: {}", createdMasterDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(masterCreateDto);
    }

    @Operation(
            summary = "Delete a service",
            description = "Delete a service",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Successfully deleted service"),
                    @ApiResponse(responseCode = "400", description = "Bad request"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @DeleteMapping()
    public ResponseEntity<MasterCreateDto> delete(@RequestBody MasterCreateDto masterCreateDto) {
        log.info("Received request to create master: {}", masterCreateDto);
        Master master = mapper.masterCreateDtoToMaster(masterCreateDto);
        Master createdMaster = masterService.create(master);
        MasterCreateDto createdMasterDto = mapper.masterToMasterCreateDto(createdMaster);
        log.debug("Master created: {}", createdMasterDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(masterCreateDto);
    }

    @ExceptionHandler({MasterInvalidArgumentException.class, MasterNotFoundException.class})
    public ResponseEntity<String> handleProductException(Exception exception) {
        HttpStatus status = (exception instanceof MasterInvalidArgumentException) ? HttpStatus.BAD_REQUEST : HttpStatus.NOT_FOUND;
        log.error("Error occurred: {}", exception.getMessage());
        return ResponseEntity.status(status).body(exception.getMessage());
    }
}
