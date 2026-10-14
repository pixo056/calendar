package ru.top105.calendar.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import java.time.LocalDate;
import java.time.LocalTime;

public record EventRequest(
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 100) String subject,
        @NotNull EventType type,
        @NotNull LocalDate date,
        LocalTime time,
        @Size(max = 120) String location,
        @Size(max = 2000) String description) {
    @AssertTrue(message = "Дата должна быть между 1900 и 9999 годом")
    public boolean isDateWithinRange() {
        return date == null || (date.getYear() >= 1900 && date.getYear() <= 9999);
    }
}
