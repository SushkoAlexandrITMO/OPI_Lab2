package ru.itmo.lab1.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.itmo.lab1.dto.PersonDto;
import ru.itmo.lab1.dto.PersonRequest;
import ru.itmo.lab1.service.PersonService;

import java.util.List;

@RestController
@RequestMapping("/api/persons")
public class PersonController {

    private final PersonService service;

    public PersonController(PersonService service) {
        this.service = service;
    }

    @GetMapping
    public List<PersonDto> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public PersonDto get(@PathVariable long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PersonDto create(@Valid @RequestBody PersonRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public PersonDto update(@PathVariable long id, @Valid @RequestBody PersonRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id,
                       @RequestParam(required = false) Long replacementId,
                       @RequestParam(defaultValue = "false") boolean unlink) {
        service.delete(id, replacementId, unlink);
    }
}
