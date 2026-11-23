package ru.itmo.lab1.repository;

import jakarta.persistence.criteria.Path;
import ru.itmo.lab1.domain.Color;
import ru.itmo.lab1.domain.Country;
import ru.itmo.lab1.domain.FormOfEducation;
import ru.itmo.lab1.domain.Semester;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

public enum StudyGroupColumn {
    ID("id", Kind.OTHER, j -> j.root().get("id")),
    NAME("name", Kind.STRING, j -> j.root().get("name")),
    COORDINATES_X("coordinates.x", Kind.OTHER, j -> j.coordinates().get("x")),
    COORDINATES_Y("coordinates.y", Kind.OTHER, j -> j.coordinates().get("y")),
    CREATION_DATE("creationDate", Kind.OTHER, j -> j.root().get("creationDate")),
    STUDENTS_COUNT("studentsCount", Kind.OTHER, j -> j.root().get("studentsCount")),
    EXPELLED_STUDENTS("expelledStudents", Kind.OTHER, j -> j.root().get("expelledStudents")),
    TRANSFERRED_STUDENTS("transferredStudents", Kind.OTHER, j -> j.root().get("transferredStudents")),
    FORM_OF_EDUCATION("formOfEducation", FormOfEducation.class, j -> j.root().get("formOfEducation")),
    SHOULD_BE_EXPELLED("shouldBeExpelled", Kind.OTHER, j -> j.root().get("shouldBeExpelled")),
    AVERAGE_MARK("averageMark", Kind.OTHER, j -> j.root().get("averageMark")),
    SEMESTER("semesterEnum", Semester.class, j -> j.root().get("semesterEnum")),
    ADMIN_NAME("groupAdmin.name", Kind.STRING, j -> j.admin().get("name")),
    ADMIN_EYE_COLOR("groupAdmin.eyeColor", Color.class, j -> j.admin().get("eyeColor")),
    ADMIN_HAIR_COLOR("groupAdmin.hairColor", Color.class, j -> j.admin().get("hairColor")),
    ADMIN_WEIGHT("groupAdmin.weight", Kind.OTHER, j -> j.admin().get("weight")),
    ADMIN_PASSPORT_ID("groupAdmin.passportId", Kind.STRING, j -> j.admin().get("passportId")),
    ADMIN_NATIONALITY("groupAdmin.nationality", Country.class, j -> j.admin().get("nationality")),
    ADMIN_LOCATION_NAME("groupAdmin.location.name", Kind.STRING, j -> j.location().get("name"));

    public enum Kind { STRING, ENUM, OTHER }

    private final String key;
    private final Kind kind;
    private final Class<? extends Enum<?>> enumType;
    private final Function<StudyGroupJoins, Path<?>> pathFactory;

    StudyGroupColumn(String key, Kind kind, Function<StudyGroupJoins, Path<?>> pathFactory) {
        this.key = key;
        this.kind = kind;
        this.enumType = null;
        this.pathFactory = pathFactory;
    }

    StudyGroupColumn(String key, Class<? extends Enum<?>> enumType, Function<StudyGroupJoins, Path<?>> pathFactory) {
        this.key = key;
        this.kind = Kind.ENUM;
        this.enumType = enumType;
        this.pathFactory = pathFactory;
    }

    public static Optional<StudyGroupColumn> byKey(String key) {
        return Arrays.stream(values()).filter(c -> c.key.equals(key)).findFirst();
    }

    public boolean isFilterable() {
        return kind != Kind.OTHER;
    }

    public String getKey() {
        return key;
    }

    public Kind getKind() {
        return kind;
    }

    public Class<? extends Enum<?>> getEnumType() {
        return enumType;
    }

    public Path<?> path(StudyGroupJoins joins) {
        return pathFactory.apply(joins);
    }
}
