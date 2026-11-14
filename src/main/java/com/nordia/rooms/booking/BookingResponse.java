package com.nordia.rooms.booking;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        Long roomId,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime start,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime end,
        String organiser,
        String subject,
        BookingStatus status) {

    static BookingResponse from(Booking b) {
        return new BookingResponse(b.getId(), b.getRoom().getId(), b.getStartTime(), b.getEndTime(),
                b.getOrganiser(), b.getSubject(), b.getStatus());
    }
}