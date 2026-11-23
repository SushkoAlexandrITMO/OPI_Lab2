package ru.itmo.lab1.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ru.itmo.lab1.domain.Color;
import ru.itmo.lab1.domain.Country;


public record PersonRequest(
        @NotBlank(message = "Имя не может быть пустым") String name,
        Color eyeColor,
        @NotNull(message = "Цвет волос обязателен") Color hairColor,
        Long locationId,
        @Valid LocationRequest location,
        @NotNull(message = "Вес обязателен") @Positive(message = "Вес должен быть больше 0") Float weight,
        String passportId,
        Country nationality,
        Long version
) {
}
