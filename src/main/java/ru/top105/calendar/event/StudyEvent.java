package ru.top105.calendar.event;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "study_events")
public class StudyEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String title;
    @Column(nullable = false, length = 100)
    private String subject;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private EventType type;
    @Column(name = "event_date", nullable = false)
    private LocalDate date;
    @Column(name = "event_time")
    private LocalTime time;
    @Column(length = 120)
    private String location;
    @Column(length = 2000)
    private String description;

    protected StudyEvent() {}

    public StudyEvent(EventRequest request) { update(request); }

    public void update(EventRequest request) {
        title = request.title().strip();
        subject = request.subject().strip();
        type = request.type();
        date = request.date();
        time = request.time();
        location = request.location() == null ? "" : request.location().strip();
        description = request.description() == null ? "" : request.description().strip();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getSubject() { return subject; }
    public EventType getType() { return type; }
    public LocalDate getDate() { return date; }
    public LocalTime getTime() { return time; }
    public String getLocation() { return location; }
    public String getDescription() { return description; }
}
