package ru.itmo.lab1.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ru.itmo.lab1.domain.FormOfEducation;
import ru.itmo.lab1.domain.Semester;


public record StudyGroupRequest(
        @NotBlank(message = "Название группы не может быть пустым") String name,
        Long coordinatesId,
        @Valid CoordinatesRequest coordinates,
        @NotNull(message = "Количество студентов обязательно")
        @Positive(message = "Количество студентов должно быть больше 0") Integer studentsCount,
        @NotNull(message = "Количество отчисленных обязательно")
        @Positive(message = "Количество отчисленных должно быть больше 0") Integer expelledStudents,
        @NotNull(message = "Количество переведённых обязательно")
        @Positive(message = "Количество переведённых должно быть больше 0") Long transferredStudents,
        @NotNull(message = "Форма обучения обязательна") FormOfEducation formOfEducation,
        @NotNull(message = "Поле shouldBeExpelled обязательно")
        @Positive(message = "Поле shouldBeExpelled должно быть больше 0") Long shouldBeExpelled,
        @NotNull(message = "Средняя оценка обязательна")
        @Positive(message = "Средняя оценка должна быть больше 0") Long averageMark,
        @NotNull(message = "Семестр обязателен") Semester semesterEnum,
        Long groupAdminId,
        @Valid PersonRequest groupAdmin,
        Long version
) {
}
