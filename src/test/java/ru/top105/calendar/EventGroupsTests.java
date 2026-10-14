package ru.top105.calendar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;
import ru.top105.calendar.group.EventGroups;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class EventGroupsTests {
    @TempDir Path directory;

    @Test void legacyEventsRemainInDefaultGroupWithoutRewritingAnything() throws Exception {
        Path file = directory.resolve("groups.properties");
        EventGroups groups = new EventGroups(file.toString());
        assertTrue(groups.belongsTo(1L, "ТОП-105Б"));
        assertFalse(groups.belongsTo(1L, "ТОП-106Б"));
        groups.assign(1L, "ТОП-105Б");
        assertFalse(Files.exists(file));
    }

    @Test void remembersOwnershipAfterReloadAndKeepsExistingAssignments() throws Exception {
        Path file = directory.resolve("groups.properties");
        EventGroups groups = new EventGroups(file.toString());
        groups.assign(3L, "ТОП-106Б");
        groups.assign(4L, "ТОП-107Б");
        EventGroups reloaded = new EventGroups(file.toString());
        assertTrue(reloaded.belongsTo(3L, "ТОП-106Б"));
        assertFalse(reloaded.belongsTo(3L, "ТОП-105Б"));
        assertTrue(reloaded.belongsTo(4L, "ТОП-107Б"));
        assertTrue(reloaded.belongsTo(2L, "ТОП-105Б"));
    }

    @Test void reportsWriteFailureWithoutChangingOwnership() throws Exception {
        Path obstacle = directory.resolve("not-a-directory");
        Files.writeString(obstacle, "keep");
        EventGroups groups = new EventGroups(obstacle.resolve("groups.properties").toString());
        assertThrows(ResponseStatusException.class, () -> groups.assign(7L, "ТОП-106Б"));
        assertTrue(groups.belongsTo(7L, "ТОП-105Б"));
        assertEquals("keep", Files.readString(obstacle));
    }
}
