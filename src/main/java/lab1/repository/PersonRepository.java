package ru.itmo.lab1.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.itmo.lab1.domain.Location;
import ru.itmo.lab1.domain.Person;

import java.util.List;

public interface PersonRepository extends JpaRepository<Person, Long> {

    List<Person> findAllByOrderByIdAsc();

    long countByLocation(Location location);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Person p SET p.location = :to, p.version = p.version + 1 WHERE p.location = :from")
    int reassignLocation(@Param("from") Location from, @Param("to") Location to);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Person p SET p.location = NULL, p.version = p.version + 1 WHERE p.location = :from")
    int unlinkLocation(@Param("from") Location from);
}
