package com.nordia.rooms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.security")
public record AppUsersProperties(List<UserEntry> users) {

    public record UserEntry(String username, String password, List<String> roles) {
    }
}