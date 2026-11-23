package ru.itmo.lab1.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.lab1.domain.Person;
import ru.itmo.lab1.dto.PersonDto;
import ru.itmo.lab1.dto.PersonRequest;
import ru.itmo.lab1.exception.BadRequestException;
import ru.itmo.lab1.exception.NotFoundException;
import ru.itmo.lab1.exception.ReferencedEntityException;
import ru.itmo.lab1.mapper.DtoMapper;
import ru.itmo.lab1.repository.PersonRepository;
import ru.itmo.lab1.repository.StudyGroupRepository;

import java.util.List;

import static ru.itmo.lab1.dto.EntityChangedEvent.Action.*;
import static ru.itmo.lab1.dto.EntityChangedEvent.EntityType.PERSON;
import static ru.itmo.lab1.dto.EntityChangedEvent.EntityType.STUDY_GROUP;

@Service
@Transactional
public class PersonService {

    private final PersonRepository repository;
    private final StudyGroupRepository studyGroupRepository;
    private final LocationService locationService;
    private final ChangeNotifier notifier;

    public PersonService(PersonRepository repository,
                         StudyGroupRepository studyGroupRepository,
                         LocationService locationService,
                         ChangeNotifier notifier) {
        this.repository = repository;
        this.studyGroupRepository = studyGroupRepository;
        this.locationService = locationService;
        this.notifier = notifier;
    }

    @Transactional(readOnly = true)
    public List<PersonDto> findAll() {
        return repository.findAllByOrderByIdAsc().stream().map(DtoMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public PersonDto get(long id) {
        return DtoMapper.toDto(getEntity(id));
    }

    public PersonDto create(PersonRequest request) {
        return DtoMapper.toDto(createEntity(request, ""));
    }

    public PersonDto update(long id, PersonRequest request) {
        Person person = getEntity(id);
        ServiceUtils.checkVersion(request.version(), person.getVersion());
        apply(person, request, "");
        repository.saveAndFlush(person);
        notifier.publish(PERSON, UPDATED, id);
        notifier.publish(STUDY_GROUP, UPDATED, null);
        return DtoMapper.toDto(person);
    }

    public void delete(long id, Long replacementId, boolean unlink) {
        Person target = getEntity(id);
        long references = studyGroupRepository.countByGroupAdmin(target);
        if (references > 0) {
            if (replacementId != null) {
                ServiceUtils.checkReplacement(id, replacementId);
                studyGroupRepository.reassignGroupAdmin(target, getEntity(replacementId));
            } else if (unlink) {
                studyGroupRepository.unlinkGroupAdmin(target);
            } else {
                throw new ReferencedEntityException(
                        "Человек является администратором групп (%d). Выберите нового администратора или оставьте группы без него"
                                .formatted(references),
                        references, true);
            }
            notifier.publish(STUDY_GROUP, UPDATED, null);
        }
        repository.deleteById(id);
        repository.flush();
        notifier.publish(PERSON, DELETED, id);
    }

    /** Для формы группы: существующий человек, новый или никакого. */
    Person resolveOptional(Long existingId, PersonRequest newPerson) {
        if (existingId != null && newPerson != null) {
            throw BadRequestException.field("groupAdmin",
                    "Укажите либо существующего администратора, либо нового, но не оба варианта");
        }
        if (existingId != null) {
            return repository.findById(existingId).orElseThrow(() ->
                    BadRequestException.field("groupAdminId", "Человек с id %d не найден".formatted(existingId)));
        }
        return newPerson == null ? null : createEntity(newPerson, "groupAdmin");
    }

    Person getEntity(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Человек с id %d не найден".formatted(id)));
    }

    private Person createEntity(PersonRequest request, String fieldPrefix) {
        Person person = new Person();
        apply(person, request, fieldPrefix);
        repository.saveAndFlush(person);
        notifier.publish(PERSON, CREATED, person.getId());
        return person;
    }

    private void apply(Person person, PersonRequest request, String fieldPrefix) {
        person.setName(request.name().trim());
        person.setEyeColor(request.eyeColor());
        person.setHairColor(request.hairColor());
        person.setLocation(locationService.resolveOptional(request.locationId(), request.location(), fieldPrefix));
        person.setWeight(request.weight());
        person.setPassportId(ServiceUtils.trimToNull(request.passportId()));
        person.setNationality(request.nationality());
    }
}
