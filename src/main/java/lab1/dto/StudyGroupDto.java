package ru.itmo.lab1.dto;

import ru.itmo.lab1.domain.FormOfEducation;
import ru.itmo.lab1.domain.Semester;

import java.time.ZonedDateTime;

public record StudyGroupDto(
        Long id,
        String name,
        CoordinatesDto coordinates,
        ZonedDateTime creationDate,
        int studentsCount,
        int expelledStudents,
        Long transferredStudents,
        FormOfEducation formOfEducation,
        Long shouldBeExpelled,
        Long averageMark,
        Semester semesterEnum,
        PersonDto groupAdmin,
        Long version
) {
}
