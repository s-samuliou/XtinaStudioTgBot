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
@Tag(name="Salon Controller", description="Handles operations related to salon")
@RestController
@RequestMapping("/v1/salon")
public class SalonController {

    @Autowired
    MasterService masterService;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private MasterMapper mapper;

    @Operation(
            summary = "Create a new salon",
            description = "Creates a new salon based on the provided data",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Successfully created salon"),
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
            summary = "Get all salon",
            description = "Gets all salon",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Successfully created salon"),
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
            summary = "Update a salon",
            description = "Update a salon",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Successfully updated salon"),
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
            summary = "Delete a salon",
            description = "Delete a salon",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Successfully deleted salon"),
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
