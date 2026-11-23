package ru.itmo.lab1.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;


@Converter
public class ZonedDateTimeConverter implements AttributeConverter<ZonedDateTime, Timestamp> {

    @Override
    public Timestamp convertToDatabaseColumn(ZonedDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    @Override
    public ZonedDateTime convertToEntityAttribute(Timestamp value) {
        return value == null ? null : value.toInstant().atZone(ZoneId.systemDefault());
    }
}
