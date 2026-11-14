package com.nordia.rooms.booking;

import com.nordia.rooms.common.ApiException;
import com.nordia.rooms.room.Room;
import com.nordia.rooms.room.RoomService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.nordia.rooms.booking.BookingStatus.ACTIVE;
import static com.nordia.rooms.booking.BookingStatus.CANCELLED;
import static com.nordia.rooms.common.ErrorCode.*;

@Service
public class BookingService {

    private final BookingRepository bookings;
    private final RoomService rooms;
    private final BookingPolicy policy;
    private final Clock clock;

    public BookingService(BookingRepository bookings, RoomService rooms, BookingPolicy policy, Clock clock) {
        this.bookings = bookings;
        this.rooms = rooms;
        this.policy = policy;
        this.clock = clock;
    }

    @Transactional
    public BookingResponse create(CreateBookingRequest request, String organiser) {
        Room room = rooms.require(request.roomId());
        LocalDateTime now = LocalDateTime.now(clock);
        policy.validateSlot(request.start(), request.end(), now);

        if (bookings.countOverlapping(room.getId(), request.start(), request.end(), ACTIVE) > 0) {
            throw new ApiException(ROOM_ALREADY_BOOKED,
                    "Room " + room.getName() + " is already booked during this time.");
        }

        LocalDate day = request.start().toLocalDate();
        long activeThatDay = bookings.countForOrganiserOnDay(
                organiser, day.atStartOfDay(), day.plusDays(1).atStartOfDay(), ACTIVE);
        if (activeThatDay >= BookingPolicy.MAX_ACTIVE_PER_DAY) {
            throw new ApiException(DAILY_LIMIT_REACHED,
                    "An organiser cannot have more than " + BookingPolicy.MAX_ACTIVE_PER_DAY + " active bookings on the same day.");
        }

        Booking saved = bookings.save(new Booking(room, request.start(), request.end(),
                organiser, request.subject().strip(), now));
        return BookingResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public BookingResponse get(Long id) {
        return BookingResponse.from(require(id));
    }

    @Transactional
    public void cancel(Long id, String username, boolean isAdmin) {
        Booking booking = require(id);
        if (!isAdmin && !booking.getOrganiser().equals(username)) {
            throw new ApiException(NOT_BOOKING_ORGANISER, "Only the organiser or an admin can cancel this booking.");
        }
        if (booking.getStatus() == CANCELLED) {
            throw new ApiException(BOOKING_ALREADY_CANCELLED, "This booking is already cancelled.");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (!now.isBefore(booking.getStartTime())) {
            throw new ApiException(BOOKING_ALREADY_STARTED, "A booking that has already started cannot be cancelled.");
        }
        booking.cancel(now);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> listForRoomOnDay(Long roomId, LocalDate date) {
        rooms.require(roomId);
        return bookings.findForRoomOnDay(roomId, date.atStartOfDay(), date.plusDays(1).atStartOfDay(), ACTIVE)
                .stream().map(BookingResponse::from).toList();
    }

    private Booking require(Long id) {
        return bookings.findById(id).orElseThrow(() ->
                new ApiException(BOOKING_NOT_FOUND, "Booking " + id + " does not exist."));
    }
}