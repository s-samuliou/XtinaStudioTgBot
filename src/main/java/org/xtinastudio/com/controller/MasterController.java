package org.xtinastudio.com.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xtinastudio.com.entity.Master;
import org.xtinastudio.com.exceptions.MasterInvalidArgumentException;
import org.xtinastudio.com.exceptions.MasterNotFoundException;
import org.xtinastudio.com.service.MasterService;

@Slf4j
@Tag(name="Product Controller", description="Handles operations related to products")
@RestController
@RequestMapping("/v1/masters")
public class MasterController {

    @Autowired
    MasterService masterService;

    @Operation(
            summary = "Create a new product",
            description = "Creates a new product based on the provided data",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Successfully created product"),
                    @ApiResponse(responseCode = "400", description = "Bad request"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @PostMapping()
    public ResponseEntity<Master> create(@RequestBody Master master) {
        log.info("Received request to create product: {}", master);
        Master createMaster = masterService.create(master);
        log.debug("Product created: {}", createMaster);
        return ResponseEntity.status(HttpStatus.CREATED).body(createMaster);
    }




    @ExceptionHandler({MasterInvalidArgumentException.class, MasterNotFoundException.class})
    public ResponseEntity<String> handleProductException(Exception exception) {
        HttpStatus status = (exception instanceof MasterInvalidArgumentException) ? HttpStatus.BAD_REQUEST : HttpStatus.NOT_FOUND;
        log.error("Error occurred: {}", exception.getMessage());
        return ResponseEntity.status(status).body(exception.getMessage());
    }
}
