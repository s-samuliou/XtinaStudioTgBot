package org.xtinastudio.com.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.xtinastudio.com.entity.Appointment;
import org.xtinastudio.com.entity.MasterReview;
import org.xtinastudio.com.entity.Salon;
import org.xtinastudio.com.entity.Services;
import org.xtinastudio.com.enums.Role;
import org.xtinastudio.com.enums.WorkStatus;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MasterCreateDto {

    @Schema(description = "The unique identifier of the master", example = "3")
    private Long id;

    @Schema(description = "The identifier of the master's chat", example = "1")
    private Long chatId;

    @Schema(description = "The name of the master", example = "Stepan")
    private String name;

    @Schema(description = "The master's login", example = "stepan@gmail.com")
    private String login;

    @Schema(description = "The master's password", example = "qwerty")
    private String password;

    @Schema(description = "The master's url", example = "//inst")
    private String url;

    @Schema(description = "The master's description", example = "Super Master")
    private String description;

    @Schema(description = "The master's role", example = "MASTER")
    private Role role;

    @Schema(description = "The master's work status", example = "WORKING")
    private WorkStatus workStatus;

    private Integer serviceId;

    private Integer salonId;
}
