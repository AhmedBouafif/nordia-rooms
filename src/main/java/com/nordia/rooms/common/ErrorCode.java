package com.nordia.rooms.common;

public enum ErrorCode {
    ROOM_NOT_FOUND(404),
    BOOKING_NOT_FOUND(404),
    NOT_BOOKING_ORGANISER(403),
    TIME_NOT_ON_MINUTE(422),
    INVALID_TIME_RANGE(422),
    START_IN_PAST(422),
    OUTSIDE_OPENING_HOURS(422),
    INVALID_DURATION(422),
    ROOM_ALREADY_BOOKED(409),
    DAILY_LIMIT_REACHED(409),
    BOOKING_ALREADY_CANCELLED(409),
    BOOKING_ALREADY_STARTED(409);

    private final int status;

    ErrorCode(int status) { this.status = status; }

    public int status() { return status; }
}