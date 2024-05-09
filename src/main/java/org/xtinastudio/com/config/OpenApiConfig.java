package org.xtinastudio.com.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@OpenAPIDefinition (
        info = @Info(
                title = "xtina.studio api",
                description = "Beauty saloon", version = "1.0.0",
                contact = @Contact(
                        name = "Stsiapan Samuliou",
                        url = "https://github.com/s-samuliou/XtinaStudioTgBot")

        )
)
@SecurityScheme(
        name = "basicauth",
        in = SecuritySchemeIn.HEADER,
        type = SecuritySchemeType.HTTP,
        scheme = "basic"
)
public class OpenApiConfig {
}
