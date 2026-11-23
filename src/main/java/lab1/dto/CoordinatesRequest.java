package ru.itmo.lab1.dto;

import jakarta.validation.constraints.NotNull;

public record CoordinatesRequest(
        @NotNull(message = "Координата x обязательна") Float x,
        @NotNull(message = "Координата y обязательна") Double y,
        Long version
) {
}
