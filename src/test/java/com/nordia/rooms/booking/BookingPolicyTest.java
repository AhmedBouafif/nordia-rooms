package com.nordia.rooms.booking;

import com.nordia.rooms.common.ApiException;
import com.nordia.rooms.common.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.nordia.rooms.common.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.*;

class BookingPolicyTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 16, 7, 0); // Monday
    private static final LocalDate MON = NOW.toLocalDate();

    private final BookingPolicy policy = new BookingPolicy();

    private ErrorCode violationOf(LocalDateTime start, LocalDateTime end) {
        return assertThrows(ApiException.class, () -> policy.validateSlot(start, end, NOW)).getCode();
    }

    @Test
    void acceptsExactBoundaries() {
        assertDoesNotThrow(() -> policy.validateSlot(MON.atTime(8, 0), MON.atTime(8, 15), NOW));  // opens 08:00, 15 min
        assertDoesNotThrow(() -> policy.validateSlot(MON.atTime(15, 0), MON.atTime(19, 0), NOW)); // closes 19:00, 4 h
    }

    @Test
    void rejectsWeekends() {
        assertEquals(OUTSIDE_OPENING_HOURS, violationOf(MON.plusDays(5).atTime(10, 0), MON.plusDays(5).atTime(11, 0)));
        assertEquals(OUTSIDE_OPENING_HOURS, violationOf(MON.plusDays(6).atTime(10, 0), MON.plusDays(6).atTime(11, 0)));
    }

    @Test
    void rejectsOutsideOpeningHours() {
        assertEquals(OUTSIDE_OPENING_HOURS, violationOf(MON.atTime(7, 45), MON.atTime(8, 30)));
        assertEquals(OUTSIDE_OPENING_HOURS, violationOf(MON.atTime(18, 30), MON.atTime(19, 15)));
        assertEquals(OUTSIDE_OPENING_HOURS, violationOf(MON.atTime(18, 0), MON.plusDays(1).atTime(9, 0)));
    }

    @Test
    void rejectsDurationOutOfBounds() {
        assertEquals(INVALID_DURATION, violationOf(MON.atTime(10, 0), MON.atTime(10, 14)));
        assertEquals(INVALID_DURATION, violationOf(MON.atTime(10, 0), MON.atTime(14, 1)));
    }

    @Test
    void rejectsEndNotAfterStart() {
        assertEquals(INVALID_TIME_RANGE, violationOf(MON.atTime(10, 0), MON.atTime(10, 0)));
        assertEquals(INVALID_TIME_RANGE, violationOf(MON.atTime(11, 0), MON.atTime(10, 0)));
    }

    @Test
    void rejectsStartInThePast() {
        assertEquals(START_IN_PAST, violationOf(MON.minusDays(7).atTime(10, 0), MON.minusDays(7).atTime(11, 0)));
        assertEquals(START_IN_PAST, violationOf(NOW, NOW.plusMinutes(30)));
    }

    @Test
    void rejectsSecondsPrecision() {
        assertEquals(TIME_NOT_ON_MINUTE, violationOf(MON.atTime(10, 0, 30), MON.atTime(11, 0)));
    }
}