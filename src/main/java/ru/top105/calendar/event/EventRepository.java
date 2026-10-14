package ru.top105.calendar.event;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventRepository extends JpaRepository<StudyEvent, Long> {
    List<StudyEvent> findAllByOrderByDateAscTimeAscIdAsc();
}
