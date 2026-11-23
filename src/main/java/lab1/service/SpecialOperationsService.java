package ru.itmo.lab1.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.lab1.domain.StudyGroup;
import ru.itmo.lab1.dto.IdCountDto;
import ru.itmo.lab1.dto.StudyGroupDto;
import ru.itmo.lab1.exception.NotFoundException;
import ru.itmo.lab1.mapper.DtoMapper;
import ru.itmo.lab1.repository.SpecialOperationsRepository;
import ru.itmo.lab1.repository.StudyGroupRepository;

import java.util.Comparator;
import java.util.List;

import static ru.itmo.lab1.dto.EntityChangedEvent.Action.UPDATED;
import static ru.itmo.lab1.dto.EntityChangedEvent.EntityType.STUDY_GROUP;

@Service
@Transactional
public class SpecialOperationsService {

    private final SpecialOperationsRepository specialRepository;
    private final StudyGroupRepository studyGroupRepository;
    private final PersonService personService;
    private final ChangeNotifier notifier;

    public SpecialOperationsService(SpecialOperationsRepository specialRepository,
                                    StudyGroupRepository studyGroupRepository,
                                    PersonService personService,
                                    ChangeNotifier notifier) {
        this.specialRepository = specialRepository;
        this.studyGroupRepository = studyGroupRepository;
        this.personService = personService;
        this.notifier = notifier;
    }

    @Transactional(readOnly = true)
    public StudyGroupDto minExpelled() {
        long id = specialRepository.findMinExpelledId()
                .orElseThrow(() -> new NotFoundException("В системе нет ни одной группы"));
        return DtoMapper.toDto(loadGroup(id));
    }

    @Transactional(readOnly = true)
    public List<IdCountDto> countById() {
        return specialRepository.countById().stream()
                .map(row -> new IdCountDto(((Number) row[0]).longValue(), ((Number) row[1]).longValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StudyGroupDto> adminGreaterThan(long personId) {
        personService.getEntity(personId); // понятная 404, если человека нет
        List<Long> ids = specialRepository.findIdsWithAdminGreaterThan(personId);
        return studyGroupRepository.findAllById(ids).stream()
                .sorted(Comparator.comparing(StudyGroup::getId))
                .map(DtoMapper::toDto)
                .toList();
    }

    public StudyGroupDto expelAll(long groupId) {
        specialRepository.expelAll(groupId);
        notifier.publish(STUDY_GROUP, UPDATED, groupId);
        return DtoMapper.toDto(loadGroup(groupId));
    }

    public StudyGroupDto addStudent(long groupId) {
        specialRepository.addStudent(groupId);
        notifier.publish(STUDY_GROUP, UPDATED, groupId);
        return DtoMapper.toDto(loadGroup(groupId));
    }

    private StudyGroup loadGroup(long id) {
        return studyGroupRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Группа с id %d не найдена".formatted(id)));
    }
}
