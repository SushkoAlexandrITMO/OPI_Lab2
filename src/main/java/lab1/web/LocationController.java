package ru.itmo.lab1.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.itmo.lab1.dto.LocationDto;
import ru.itmo.lab1.dto.LocationRequest;
import ru.itmo.lab1.service.LocationService;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationService service;

    public LocationController(LocationService service) {
        this.service = service;
    }

    @GetMapping
    public List<LocationDto> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public LocationDto get(@PathVariable long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LocationDto create(@Valid @RequestBody LocationRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public LocationDto update(@PathVariable long id, @Valid @RequestBody LocationRequest request) {
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
