package com.nordia.rooms.common;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Converter
public class LocalDateTimeTextConverter implements AttributeConverter<LocalDateTime, String> {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Override
    public String convertToDatabaseColumn(LocalDateTime value) {
        return value == null ? null : value.format(FORMAT);
    }

    @Override
    public LocalDateTime convertToEntityAttribute(String value) {
        return value == null ? null : LocalDateTime.parse(value, FORMAT);
    }
}