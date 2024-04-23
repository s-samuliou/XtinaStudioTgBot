package org.xtinastudio.com.tg.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "masterbot")
public record MasterBotProperties(String name, String token) {
}
