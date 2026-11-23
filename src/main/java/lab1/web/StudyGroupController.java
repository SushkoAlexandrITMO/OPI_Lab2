package ru.itmo.lab1.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.itmo.lab1.dto.PageResponse;
import ru.itmo.lab1.dto.StudyGroupDto;
import ru.itmo.lab1.dto.StudyGroupRequest;
import ru.itmo.lab1.service.StudyGroupService;

@RestController
@RequestMapping("/api/study-groups")
public class StudyGroupController {

    private final StudyGroupService service;

    public StudyGroupController(StudyGroupService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<StudyGroupDto> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String filterField,
            @RequestParam(required = false) String filterValue) {
        return service.search(filterField, filterValue, sort, direction, page, size);
    }

    @GetMapping("/{id}")
    public StudyGroupDto get(@PathVariable long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudyGroupDto create(@Valid @RequestBody StudyGroupRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public StudyGroupDto update(@PathVariable long id, @Valid @RequestBody StudyGroupRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        service.delete(id);
    }
}
