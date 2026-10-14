package ru.top105.calendar.group;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

@Component
public class GroupCatalog {
    public static final String DEFAULT_GROUP = "ТОП-105Б";
    private final List<String> names;
    private final Set<String> allowed;

    public GroupCatalog() throws IOException {
        try (var input = new ClassPathResource("catalog/groups.txt").getInputStream()) {
            names = new String(input.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .map(line -> line.replace("\uFEFF", "").strip())
                    .filter(line -> !line.isEmpty()).distinct().toList();
        }
        allowed = Set.copyOf(names);
        if (!allowed.contains(DEFAULT_GROUP)) throw new IllegalStateException("В справочнике нет основной группы");
    }

    public List<String> names() { return names; }

    public String validate(String group) {
        if (!allowed.contains(group)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Выберите группу из справочника УУНиТ.");
        return group;
    }
}
