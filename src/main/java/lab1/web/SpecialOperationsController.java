package ru.itmo.lab1.web;

import org.springframework.web.bind.annotation.*;
import ru.itmo.lab1.dto.IdCountDto;
import ru.itmo.lab1.dto.StudyGroupDto;
import ru.itmo.lab1.service.SpecialOperationsService;

import java.util.List;

@RestController
@RequestMapping("/api/special")
public class SpecialOperationsController {

    private final SpecialOperationsService service;

    public SpecialOperationsController(SpecialOperationsService service) {
        this.service = service;
    }

    @GetMapping("/min-expelled")
    public StudyGroupDto minExpelled() {
        return service.minExpelled();
    }

    @GetMapping("/count-by-id")
    public List<IdCountDto> countById() {
        return service.countById();
    }

    @GetMapping("/admin-greater-than")
    public List<StudyGroupDto> adminGreaterThan(@RequestParam long personId) {
        return service.adminGreaterThan(personId);
    }

    @PostMapping("/groups/{id}/expel-all")
    public StudyGroupDto expelAll(@PathVariable long id) {
        return service.expelAll(id);
    }

    @PostMapping("/groups/{id}/add-student")
    public StudyGroupDto addStudent(@PathVariable long id) {
        return service.addStudent(id);
    }
}
