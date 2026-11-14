package com.nordia.rooms.booking;

import com.nordia.rooms.common.ApiException;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static com.nordia.rooms.common.ErrorCode.*;

@Component
public class BookingPolicy {

    static final LocalTime OPENING = LocalTime.of(8, 0);
    static final LocalTime CLOSING = LocalTime.of(19, 0);
    static final Duration MIN_DURATION = Duration.ofMinutes(15);
    static final Duration MAX_DURATION = Duration.ofHours(4);
    static final int MAX_ACTIVE_PER_DAY = 3;

    public void validateSlot(LocalDateTime start, LocalDateTime end, LocalDateTime now) {
        if (!onWholeMinute(start) || !onWholeMinute(end)) {
            throw new ApiException(TIME_NOT_ON_MINUTE, "Start and end must be on a whole minute (no seconds).");
        }
        if (!end.isAfter(start)) {
            throw new ApiException(INVALID_TIME_RANGE, "End must be after start.");
        }
        if (!start.isAfter(now)) {
            throw new ApiException(START_IN_PAST, "A booking must start in the future.");
        }
        boolean sameDay = start.toLocalDate().equals(end.toLocalDate());
        boolean weekend = start.getDayOfWeek() == DayOfWeek.SATURDAY || start.getDayOfWeek() == DayOfWeek.SUNDAY;
        if (!sameDay || weekend
                || start.toLocalTime().isBefore(OPENING)
                || end.toLocalTime().isAfter(CLOSING)) {
            throw new ApiException(OUTSIDE_OPENING_HOURS, "Bookings are only possible Monday to Friday, 08:00 to 19:00.");
        }
        Duration duration = Duration.between(start, end);
        if (duration.compareTo(MIN_DURATION) < 0 || duration.compareTo(MAX_DURATION) > 0) {
            throw new ApiException(INVALID_DURATION, "A booking must last between 15 minutes and 4 hours.");
        }
    }

    private static boolean onWholeMinute(LocalDateTime t) {
        return t.getSecond() == 0 && t.getNano() == 0;
    }
}