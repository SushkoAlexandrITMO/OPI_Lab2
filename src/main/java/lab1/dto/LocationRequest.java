package ru.itmo.lab1.dto;

import jakarta.validation.constraints.NotNull;

public record LocationRequest(
        @NotNull(message = "Координата x обязательна") Float x,
        @NotNull(message = "Координата y обязательна") Double y,
        @NotNull(message = "Название локации не может быть null") String name,
        Long version
) {
}
