package ru.top105.calendar.event;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import ru.top105.calendar.group.EventGroups;
import ru.top105.calendar.group.GroupCatalog;

@RestController
@RequestMapping("/api/events")
public class EventController {
    private final EventRepository repository;
    private final EventGroups eventGroups;
    private final GroupCatalog catalog;

    public EventController(EventRepository repository, EventGroups eventGroups, GroupCatalog catalog) {
        this.repository = repository;
        this.eventGroups = eventGroups;
        this.catalog = catalog;
    }

    @GetMapping
    public List<StudyEvent> list(@RequestParam(defaultValue = GroupCatalog.DEFAULT_GROUP) String group) {
        catalog.validate(group);
        return repository.findAllByOrderByDateAscTimeAscIdAsc().stream()
                .filter(event -> eventGroups.belongsTo(event.getId(), group)).toList();
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED) @Transactional
    public StudyEvent create(@Valid @RequestBody EventRequest request,
                             @RequestParam(defaultValue = GroupCatalog.DEFAULT_GROUP) String group) {
        catalog.validate(group);
        StudyEvent event = repository.saveAndFlush(new StudyEvent(request));
        eventGroups.assign(event.getId(), group);
        return event;
    }

    @PutMapping("/{id}") @Transactional
    public StudyEvent update(@PathVariable Long id, @Valid @RequestBody EventRequest request,
                            @RequestParam(defaultValue = GroupCatalog.DEFAULT_GROUP) String group) {
        StudyEvent event = find(id, catalog.validate(group));
        event.update(request);
        return repository.save(event);
    }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional
    public void delete(@PathVariable Long id, @RequestParam(defaultValue = GroupCatalog.DEFAULT_GROUP) String group) {
        repository.delete(find(id, catalog.validate(group)));
    }

    private StudyEvent find(Long id, String group) {
        return repository.findById(id).filter(event -> eventGroups.belongsTo(event.getId(), group)).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Событие не найдено"));
    }
}
