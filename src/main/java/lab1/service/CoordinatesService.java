package ru.itmo.lab1.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.lab1.domain.Coordinates;
import ru.itmo.lab1.dto.CoordinatesDto;
import ru.itmo.lab1.dto.CoordinatesRequest;
import ru.itmo.lab1.exception.BadRequestException;
import ru.itmo.lab1.exception.NotFoundException;
import ru.itmo.lab1.exception.ReferencedEntityException;
import ru.itmo.lab1.mapper.DtoMapper;
import ru.itmo.lab1.repository.CoordinatesRepository;
import ru.itmo.lab1.repository.StudyGroupRepository;

import java.util.List;

import static ru.itmo.lab1.dto.EntityChangedEvent.Action.*;
import static ru.itmo.lab1.dto.EntityChangedEvent.EntityType.COORDINATES;
import static ru.itmo.lab1.dto.EntityChangedEvent.EntityType.STUDY_GROUP;

@Service
@Transactional
public class CoordinatesService {

    private final CoordinatesRepository repository;
    private final StudyGroupRepository studyGroupRepository;
    private final ChangeNotifier notifier;

    public CoordinatesService(CoordinatesRepository repository,
                              StudyGroupRepository studyGroupRepository,
                              ChangeNotifier notifier) {
        this.repository = repository;
        this.studyGroupRepository = studyGroupRepository;
        this.notifier = notifier;
    }

    @Transactional(readOnly = true)
    public List<CoordinatesDto> findAll() {
        return repository.findAllByOrderByIdAsc().stream().map(DtoMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public CoordinatesDto get(long id) {
        return DtoMapper.toDto(getEntity(id));
    }

    public CoordinatesDto create(CoordinatesRequest request) {
        return DtoMapper.toDto(createEntity(request));
    }

    public CoordinatesDto update(long id, CoordinatesRequest request) {
        Coordinates coordinates = getEntity(id);
        ServiceUtils.checkVersion(request.version(), coordinates.getVersion());
        apply(coordinates, request);
        repository.saveAndFlush(coordinates);
        notifier.publish(COORDINATES, UPDATED, id);
        // Координаты отображаются в таблице групп
        notifier.publish(STUDY_GROUP, UPDATED, null);
        return DtoMapper.toDto(coordinates);
    }

    public void delete(long id, Long replacementId) {
        Coordinates target = getEntity(id);
        long references = studyGroupRepository.countByCoordinates(target);
        if (references > 0) {
            if (replacementId == null) {
                throw new ReferencedEntityException(
                        "Координаты используются в группах (%d). Выберите координаты для перепривязки".formatted(references),
                        references, false);
            }
            ServiceUtils.checkReplacement(id, replacementId);
            Coordinates replacement = getEntity(replacementId);
            studyGroupRepository.reassignCoordinates(target, replacement);
            notifier.publish(STUDY_GROUP, UPDATED, null);
        }
        repository.deleteById(id);
        repository.flush();
        notifier.publish(COORDINATES, DELETED, id);
    }
    Coordinates resolve(Long existingId, CoordinatesRequest newCoordinates) {
        if (existingId != null && newCoordinates != null) {
            throw BadRequestException.field("coordinates", "Укажите либо существующие координаты, либо новые, но не оба варианта");
        }
        if (existingId != null) {
            return repository.findById(existingId).orElseThrow(() ->
                    BadRequestException.field("coordinatesId", "Координаты с id %d не найдены".formatted(existingId)));
        }
        if (newCoordinates != null) {
            return createEntity(newCoordinates);
        }
        throw BadRequestException.field("coordinates", "Выберите существующие координаты или задайте новые");
    }

    Coordinates getEntity(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Координаты с id %d не найдены".formatted(id)));
    }

    private Coordinates createEntity(CoordinatesRequest request) {
        Coordinates coordinates = new Coordinates();
        apply(coordinates, request);
        repository.saveAndFlush(coordinates);
        notifier.publish(COORDINATES, CREATED, coordinates.getId());
        return coordinates;
    }

    private static void apply(Coordinates coordinates, CoordinatesRequest request) {
        coordinates.setX(request.x());
        coordinates.setY(request.y());
    }
}
