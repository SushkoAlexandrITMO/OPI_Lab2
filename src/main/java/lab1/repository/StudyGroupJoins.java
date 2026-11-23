package ru.itmo.lab1.repository;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import ru.itmo.lab1.domain.Coordinates;
import ru.itmo.lab1.domain.Location;
import ru.itmo.lab1.domain.Person;
import ru.itmo.lab1.domain.StudyGroup;

public record StudyGroupJoins(
        Root<StudyGroup> root,
        Join<StudyGroup, Coordinates> coordinates,
        Join<StudyGroup, Person> admin,
        Join<Person, Location> location
) {
    public static StudyGroupJoins of(Root<StudyGroup> root) {
        Join<StudyGroup, Coordinates> coordinates = root.join("coordinates", JoinType.INNER);
        Join<StudyGroup, Person> admin = root.join("groupAdmin", JoinType.LEFT);
        Join<Person, Location> location = admin.join("location", JoinType.LEFT);
        return new StudyGroupJoins(root, coordinates, admin, location);
    }
}
