package ru.itmo.lab1.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.itmo.lab1.dto.CoordinatesDto;
import ru.itmo.lab1.dto.CoordinatesRequest;
import ru.itmo.lab1.service.CoordinatesService;

import java.util.List;

@RestController
@RequestMapping("/api/coordinates")
public class CoordinatesController {

    private final CoordinatesService service;

    public CoordinatesController(CoordinatesService service) {
        this.service = service;
    }

    @GetMapping
    public List<CoordinatesDto> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public CoordinatesDto get(@PathVariable long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CoordinatesDto create(@Valid @RequestBody CoordinatesRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public CoordinatesDto update(@PathVariable long id, @Valid @RequestBody CoordinatesRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id, @RequestParam(required = false) Long replacementId) {
        service.delete(id, replacementId);
    }
}
