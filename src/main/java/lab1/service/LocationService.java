package ru.itmo.lab1.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.lab1.domain.Location;
import ru.itmo.lab1.dto.LocationDto;
import ru.itmo.lab1.dto.LocationRequest;
import ru.itmo.lab1.exception.BadRequestException;
import ru.itmo.lab1.exception.NotFoundException;
import ru.itmo.lab1.exception.ReferencedEntityException;
import ru.itmo.lab1.mapper.DtoMapper;
import ru.itmo.lab1.repository.LocationRepository;
import ru.itmo.lab1.repository.PersonRepository;

import java.util.List;

import static ru.itmo.lab1.dto.EntityChangedEvent.Action.*;
import static ru.itmo.lab1.dto.EntityChangedEvent.EntityType.*;

@Service
@Transactional
public class LocationService {

    private final LocationRepository repository;
    private final PersonRepository personRepository;
    private final ChangeNotifier notifier;

    public LocationService(LocationRepository repository,
                           PersonRepository personRepository,
                           ChangeNotifier notifier) {
        this.repository = repository;
        this.personRepository = personRepository;
        this.notifier = notifier;
    }

    @Transactional(readOnly = true)
    public List<LocationDto> findAll() {
        return repository.findAllByOrderByIdAsc().stream().map(DtoMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public LocationDto get(long id) {
        return DtoMapper.toDto(getEntity(id));
    }

    public LocationDto create(LocationRequest request) {
        return DtoMapper.toDto(createEntity(request));
    }

    public LocationDto update(long id, LocationRequest request) {
        Location location = getEntity(id);
        ServiceUtils.checkVersion(request.version(), location.getVersion());
        apply(location, request);
        repository.saveAndFlush(location);
        notifier.publish(LOCATION, UPDATED, id);
        notifier.publish(PERSON, UPDATED, null);
        notifier.publish(STUDY_GROUP, UPDATED, null);
        return DtoMapper.toDto(location);
    }


    public void delete(long id, Long replacementId, boolean unlink) {
        Location target = getEntity(id);
        long references = personRepository.countByLocation(target);
        if (references > 0) {
            if (replacementId != null) {
                ServiceUtils.checkReplacement(id, replacementId);
                personRepository.reassignLocation(target, getEntity(replacementId));
            } else if (unlink) {
                personRepository.unlinkLocation(target);
            } else {
                throw new ReferencedEntityException(
                        "Локация используется людьми (%d). Выберите локацию для перепривязки или отвяжите её".formatted(references),
                        references, true);
            }
            notifier.publish(PERSON, UPDATED, null);
            notifier.publish(STUDY_GROUP, UPDATED, null);
        }
        repository.deleteById(id);
        repository.flush();
        notifier.publish(LOCATION, DELETED, id);
    }

    /** Для формы человека: существующая локация, новая или никакой. */
    Location resolveOptional(Long existingId, LocationRequest newLocation, String fieldPrefix) {
        if (existingId != null && newLocation != null) {
            throw BadRequestException.field(ServiceUtils.prefixed(fieldPrefix, "location"),
                    "Укажите либо существующую локацию, либо новую, но не оба варианта");
        }
        if (existingId != null) {
            return repository.findById(existingId).orElseThrow(() ->
                    BadRequestException.field(ServiceUtils.prefixed(fieldPrefix, "locationId"),
                            "Локация с id %d не найдена".formatted(existingId)));
        }
        return newLocation == null ? null : createEntity(newLocation);
    }

    Location getEntity(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Локация с id %d не найдена".formatted(id)));
    }

    private Location createEntity(LocationRequest request) {
        Location location = new Location();
        apply(location, request);
        repository.saveAndFlush(location);
        notifier.publish(LOCATION, CREATED, location.getId());
        return location;
    }

    private static void apply(Location location, LocationRequest request) {
        location.setX(request.x());
        location.setY(request.y());
        location.setName(request.name());
    }
}
