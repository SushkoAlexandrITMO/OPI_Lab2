package ru.itmo.lab1.dto;

import ru.itmo.lab1.domain.Color;
import ru.itmo.lab1.domain.Country;

public record PersonDto(
        Long id,
        String name,
        Color eyeColor,
        Color hairColor,
        LocationDto location,
        float weight,
        String passportId,
        Country nationality,
        Long version
) {
}
