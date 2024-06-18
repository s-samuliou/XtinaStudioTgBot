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
import org.xtinastudio.com.security.model.JwtAuthenticationResponse;
import org.xtinastudio.com.security.model.SignInRequest;
import org.xtinastudio.com.service.MasterService;

@Slf4j
@Tag(name="Master Controller", description="Handles operations related to masters")
@RestController
@RequestMapping("/v1/masters")
public class MasterController {

    @Autowired
    MasterService masterService;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private MasterMapper mapper;

    @Operation(
            summary = "Create a new master",
            description = "Creates a new master based on the provided data",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Successfully created master"),
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

    @PostMapping("/login")
    @ResponseBody
    public JwtAuthenticationResponse login(@RequestBody SignInRequest request) {
        return authenticationService.authenticate(request);
    }

    @ExceptionHandler({MasterInvalidArgumentException.class, MasterNotFoundException.class})
    public ResponseEntity<String> handleProductException(Exception exception) {
        HttpStatus status = (exception instanceof MasterInvalidArgumentException) ? HttpStatus.BAD_REQUEST : HttpStatus.NOT_FOUND;
        log.error("Error occurred: {}", exception.getMessage());
        return ResponseEntity.status(status).body(exception.getMessage());
    }
}
