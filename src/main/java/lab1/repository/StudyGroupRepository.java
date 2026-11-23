package ru.itmo.lab1.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.itmo.lab1.domain.Coordinates;
import ru.itmo.lab1.domain.Person;
import ru.itmo.lab1.domain.StudyGroup;

public interface StudyGroupRepository extends JpaRepository<StudyGroup, Long> {

    long countByCoordinates(Coordinates coordinates);

    long countByGroupAdmin(Person groupAdmin);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE StudyGroup g SET g.coordinates = :to, g.version = g.version + 1 WHERE g.coordinates = :from")
    int reassignCoordinates(@Param("from") Coordinates from, @Param("to") Coordinates to);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE StudyGroup g SET g.groupAdmin = :to, g.version = g.version + 1 WHERE g.groupAdmin = :from")
    int reassignGroupAdmin(@Param("from") Person from, @Param("to") Person to);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE StudyGroup g SET g.groupAdmin = NULL, g.version = g.version + 1 WHERE g.groupAdmin = :from")
    int unlinkGroupAdmin(@Param("from") Person from);
}
