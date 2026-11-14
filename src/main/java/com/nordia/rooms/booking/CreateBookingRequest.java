package com.nordia.rooms.booking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateBookingRequest(
        @NotNull Long roomId,
        @NotNull LocalDateTime start,
        @NotNull LocalDateTime end,
        @NotBlank @Size(max = 200) String subject) {
}