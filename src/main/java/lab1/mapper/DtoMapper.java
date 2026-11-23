package ru.itmo.lab1.mapper;

import ru.itmo.lab1.domain.Coordinates;
import ru.itmo.lab1.domain.Location;
import ru.itmo.lab1.domain.Person;
import ru.itmo.lab1.domain.StudyGroup;
import ru.itmo.lab1.dto.CoordinatesDto;
import ru.itmo.lab1.dto.LocationDto;
import ru.itmo.lab1.dto.PersonDto;
import ru.itmo.lab1.dto.StudyGroupDto;


public final class DtoMapper {

    private DtoMapper() {
    }

    public static CoordinatesDto toDto(Coordinates c) {
        if (c == null) {
            return null;
        }
        return new CoordinatesDto(c.getId(), c.getX(), c.getY(), c.getVersion());
    }

    public static LocationDto toDto(Location l) {
        if (l == null) {
            return null;
        }
        return new LocationDto(l.getId(), l.getX(), l.getY(), l.getName(), l.getVersion());
    }

    public static PersonDto toDto(Person p) {
        if (p == null) {
            return null;
        }
        return new PersonDto(
                p.getId(),
                p.getName(),
                p.getEyeColor(),
                p.getHairColor(),
                toDto(p.getLocation()),
                p.getWeight(),
                p.getPassportId(),
                p.getNationality(),
                p.getVersion()
        );
    }

    public static StudyGroupDto toDto(StudyGroup g) {
        if (g == null) {
            return null;
        }
        return new StudyGroupDto(
                g.getId(),
                g.getName(),
                toDto(g.getCoordinates()),
                g.getCreationDate(),
                g.getStudentsCount(),
                g.getExpelledStudents(),
                g.getTransferredStudents(),
                g.getFormOfEducation(),
                g.getShouldBeExpelled(),
                g.getAverageMark(),
                g.getSemesterEnum(),
                toDto(g.getGroupAdmin()),
                g.getVersion()
        );
    }
}
