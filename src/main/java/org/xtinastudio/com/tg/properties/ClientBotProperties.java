package org.xtinastudio.com.tg.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clientbot")
public record ClientBotProperties(String name, String token){
}
