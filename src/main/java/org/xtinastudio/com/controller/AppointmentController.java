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
@Tag(name="Appointment Controller", description="Handles operations related to appointment")
@RestController
@RequestMapping("/v1/appointment")
public class AppointmentController {

    @Autowired
    MasterService masterService;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private MasterMapper mapper;

    @Operation(
            summary = "Create a new appointment",
            description = "Creates a new appointment based on the provided data",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Successfully created appointment"),
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
            summary = "Get all appointments",
            description = "Gets all appointments",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Successfully gets appointments"),
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
            summary = "Update a appointment",
            description = "Update a appointment",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Successfully updated appointment"),
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
            summary = "Delete a appointment",
            description = "Delete a appointment",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Successfully deleted appointment"),
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
