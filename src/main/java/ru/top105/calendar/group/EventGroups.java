package ru.top105.calendar.group;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Properties;

/** Group ownership is stored separately, leaving the existing study_events schema unchanged. */
@Component
public class EventGroups {
    private final Path file;
    private Properties ownership = new Properties();

    public EventGroups(@Value("${calendar.group-links-file:./data/event-groups.properties}") String filename) throws IOException {
        file = Path.of(filename).toAbsolutePath().normalize();
        if (Files.exists(file)) {
            try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) { ownership.load(reader); }
            for (String key : ownership.stringPropertyNames()) {
                if (!key.matches("[1-9][0-9]*") || ownership.getProperty(key).isBlank()) {
                    throw new IOException("Некорректный файл привязки групп: " + file);
                }
            }
        }
    }

    public synchronized boolean belongsTo(Long id, String group) {
        return ownership.getProperty(id.toString(), GroupCatalog.DEFAULT_GROUP).equals(group);
    }

    public synchronized void assign(Long id, String group) {
        if (group.equals(GroupCatalog.DEFAULT_GROUP)) return;
        Properties next = new Properties();
        next.putAll(ownership);
        next.setProperty(id.toString(), group);
        Path temporary = null;
        try {
            Files.createDirectories(file.getParent());
            temporary = Files.createTempFile(file.getParent(), "event-groups-", ".tmp");
            try (var writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                next.store(writer, "Event ID to study group. Keep this file with calendar.mv.db.");
            }
            // Fail safely if atomic replacement is unavailable. Never replace a good file partially.
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            ownership = next;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Не удалось сохранить группу события. Проверьте доступ к папке data и повторите.", exception);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { /* retain failed temporary file */ }
            }
        }
    }
}
