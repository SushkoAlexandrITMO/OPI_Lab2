package ru.itmo.lab1.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.lab1.domain.StudyGroup;
import ru.itmo.lab1.dto.PageResponse;
import ru.itmo.lab1.dto.StudyGroupDto;
import ru.itmo.lab1.dto.StudyGroupRequest;
import ru.itmo.lab1.exception.BadRequestException;
import ru.itmo.lab1.exception.NotFoundException;
import ru.itmo.lab1.mapper.DtoMapper;
import ru.itmo.lab1.repository.StudyGroupColumn;
import ru.itmo.lab1.repository.StudyGroupRepository;
import ru.itmo.lab1.repository.StudyGroupSearchRepository;

import static ru.itmo.lab1.dto.EntityChangedEvent.Action.*;
import static ru.itmo.lab1.dto.EntityChangedEvent.EntityType.STUDY_GROUP;

@Service
@Transactional
public class StudyGroupService {

    private static final int MAX_PAGE_SIZE = 100;

    private final StudyGroupRepository repository;
    private final StudyGroupSearchRepository searchRepository;
    private final CoordinatesService coordinatesService;
    private final PersonService personService;
    private final ChangeNotifier notifier;

    public StudyGroupService(StudyGroupRepository repository,
                             StudyGroupSearchRepository searchRepository,
                             CoordinatesService coordinatesService,
                             PersonService personService,
                             ChangeNotifier notifier) {
        this.repository = repository;
        this.searchRepository = searchRepository;
        this.coordinatesService = coordinatesService;
        this.personService = personService;
        this.notifier = notifier;
    }

    @Transactional(readOnly = true)
    public PageResponse<StudyGroupDto> search(String filterField, String filterValue,
                                              String sortField, String direction,
                                              int page, int size) {
        StudyGroupColumn filterColumn = null;
        if (filterField != null && !filterField.isBlank()) {
            filterColumn = StudyGroupColumn.byKey(filterField)
                    .filter(StudyGroupColumn::isFilterable)
                    .orElseThrow(() -> BadRequestException.field("filterField",
                            "Фильтрация по полю '%s' не поддерживается".formatted(filterField)));
        }
        StudyGroupColumn sortColumn = StudyGroupColumn.byKey(sortField == null ? "id" : sortField)
                .orElseThrow(() -> BadRequestException.field("sort",
                        "Сортировка по полю '%s' не поддерживается".formatted(sortField)));
        boolean ascending = !"desc".equalsIgnoreCase(direction);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        StudyGroupSearchRepository.SearchResult result = searchRepository.search(
                filterColumn, filterValue == null ? null : filterValue.trim(),
                sortColumn, ascending, safePage, safeSize);

        int totalPages = (int) ((result.total() + safeSize - 1) / safeSize);
        return new PageResponse<>(
                result.content().stream().map(DtoMapper::toDto).toList(),
                safePage, safeSize, result.total(), totalPages);
    }

    @Transactional(readOnly = true)
    public StudyGroupDto get(long id) {
        return DtoMapper.toDto(getEntity(id));
    }

    public StudyGroupDto create(StudyGroupRequest request) {
        StudyGroup group = new StudyGroup();
        apply(group, request);
        repository.saveAndFlush(group);
        notifier.publish(STUDY_GROUP, CREATED, group.getId());
        return DtoMapper.toDto(group);
    }

    public StudyGroupDto update(long id, StudyGroupRequest request) {
        StudyGroup group = getEntity(id);
        ServiceUtils.checkVersion(request.version(), group.getVersion());
        apply(group, request);
        repository.saveAndFlush(group);
        notifier.publish(STUDY_GROUP, UPDATED, id);
        return DtoMapper.toDto(group);
    }


    public void delete(long id) {
        repository.delete(getEntity(id));
        repository.flush();
        notifier.publish(STUDY_GROUP, DELETED, id);
    }

    StudyGroup getEntity(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Группа с id %d не найдена".formatted(id)));
    }

    private void apply(StudyGroup group, StudyGroupRequest request) {
        group.setName(request.name().trim());
        group.setCoordinates(coordinatesService.resolve(request.coordinatesId(), request.coordinates()));
        group.setStudentsCount(request.studentsCount());
        group.setExpelledStudents(request.expelledStudents());
        group.setTransferredStudents(request.transferredStudents());
        group.setFormOfEducation(request.formOfEducation());
        group.setShouldBeExpelled(request.shouldBeExpelled());
        group.setAverageMark(request.averageMark());
        group.setSemesterEnum(request.semesterEnum());
        group.setGroupAdmin(personService.resolveOptional(request.groupAdminId(), request.groupAdmin()));
    }
}
