package ru.top105.calendar.group;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
public class GroupController {
    private final GroupCatalog catalog;
    public GroupController(GroupCatalog catalog) { this.catalog = catalog; }

    @GetMapping("/api/groups")
    public CatalogResponse groups() {
        return new CatalogResponse(GroupCatalog.DEFAULT_GROUP, "https://isu.uust.ru/class_schedule/", "2026-10-14", catalog.names());
    }

    public record CatalogResponse(String defaultGroup, String source, String updatedOn, List<String> groups) {}
}
