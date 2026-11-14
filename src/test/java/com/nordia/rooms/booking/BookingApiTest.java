package com.nordia.rooms.booking;

import com.nordia.rooms.support.ApiTestBase;
import org.junit.jupiter.api.Test;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BookingApiTest extends ApiTestBase {


    @Test
    void createsBookingForAuthenticatedOrganiser() throws Exception {
        book("alice", 1, at(MONDAY, 10, 0), at(MONDAY, 11, 0))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.roomId").value(1))
                .andExpect(jsonPath("$.organiser").value("alice"))
                .andExpect(jsonPath("$.start").value("2026-11-16T10:00:00"))
                .andExpect(jsonPath("$.end").value("2026-11-16T11:00:00"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void rejectsOverlappingBookingInSameRoom() throws Exception {
        bookOk("alice", 1, at(MONDAY, 10, 0), at(MONDAY, 11, 0));

        book("bob", 1, at(MONDAY, 10, 30), at(MONDAY, 11, 30))   // partial overlap
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ROOM_ALREADY_BOOKED"));
        book("bob", 1, at(MONDAY, 9, 0), at(MONDAY, 12, 0))      // encloses existing
                .andExpect(status().isConflict());
        book("bob", 1, at(MONDAY, 10, 15), at(MONDAY, 10, 45))   // inside existing
                .andExpect(status().isConflict());
    }

    @Test
    void allowsTouchingBookings() throws Exception {
        bookOk("alice", 1, at(MONDAY, 10, 0), at(MONDAY, 11, 0));
        book("bob", 1, at(MONDAY, 11, 0), at(MONDAY, 12, 0)).andExpect(status().isCreated());
        book("bob", 1, at(MONDAY, 9, 0), at(MONDAY, 10, 0)).andExpect(status().isCreated());
    }

    @Test
    void allowsSameSlotInAnotherRoom() throws Exception {
        bookOk("alice", 1, at(MONDAY, 10, 0), at(MONDAY, 11, 0));
        book("bob", 2, at(MONDAY, 10, 0), at(MONDAY, 11, 0)).andExpect(status().isCreated());
    }

    @Test
    void cancelledBookingFreesTheSlot() throws Exception {
        long id = bookOk("alice", 1, at(MONDAY, 10, 0), at(MONDAY, 11, 0));
        cancel("alice", id).andExpect(status().isNoContent());
        book("bob", 1, at(MONDAY, 10, 0), at(MONDAY, 11, 0)).andExpect(status().isCreated());
    }

    @Test
    void mapsBusinessRuleViolationTo422() throws Exception {
        book("alice", 1, at(MONDAY.plusDays(5), 10, 0), at(MONDAY.plusDays(5), 11, 0))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("OUTSIDE_OPENING_HOURS"));
    }

    @Test
    void returns404ForUnknownRoom() throws Exception {
        book("alice", 99, at(MONDAY, 10, 0), at(MONDAY, 11, 0))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"));
    }


    @Test
    void enforcesThreeActiveBookingsPerOrganiserPerDay() throws Exception {
        bookOk("alice", 1, at(MONDAY, 9, 0), at(MONDAY, 10, 0));
        bookOk("alice", 2, at(MONDAY, 10, 0), at(MONDAY, 11, 0));
        bookOk("alice", 3, at(MONDAY, 11, 0), at(MONDAY, 12, 0));

        book("alice", 4, at(MONDAY, 13, 0), at(MONDAY, 14, 0))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DAILY_LIMIT_REACHED"));

        book("bob", 4, at(MONDAY, 13, 0), at(MONDAY, 14, 0)).andExpect(status().isCreated());
        book("alice", 4, at(MONDAY.plusDays(1), 13, 0), at(MONDAY.plusDays(1), 14, 0)).andExpect(status().isCreated());
    }

    @Test
    void cancelledBookingsDoNotCountTowardDailyLimit() throws Exception {
        long first = bookOk("alice", 1, at(MONDAY, 9, 0), at(MONDAY, 10, 0));
        bookOk("alice", 2, at(MONDAY, 10, 0), at(MONDAY, 11, 0));
        bookOk("alice", 3, at(MONDAY, 11, 0), at(MONDAY, 12, 0));
        cancel("alice", first).andExpect(status().isNoContent());

        book("alice", 4, at(MONDAY, 13, 0), at(MONDAY, 14, 0)).andExpect(status().isCreated());
    }

    @Test
    void bookingsAlreadyPastThatDayStillCountTowardDailyLimit() throws Exception {
        bookOk("alice", 1, at(MONDAY, 8, 0), at(MONDAY, 9, 0));
        bookOk("alice", 2, at(MONDAY, 9, 0), at(MONDAY, 10, 0));
        bookOk("alice", 3, at(MONDAY, 10, 0), at(MONDAY, 11, 0));

        clock.setNow(MONDAY.atTime(12, 0)); // all three are now in the past

        book("alice", 4, at(MONDAY, 14, 0), at(MONDAY, 15, 0))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DAILY_LIMIT_REACHED"));
    }


    @Test
    void cancelsFutureBookingOnlyOnce() throws Exception {
        long id = bookOk("alice", 1, at(MONDAY, 10, 0), at(MONDAY, 11, 0));

        cancel("alice", id).andExpect(status().isNoContent());
        mvc.perform(get("/api/bookings/{id}", id).with(httpBasic("alice", "alice-pass")))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        cancel("alice", id)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOKING_ALREADY_CANCELLED"));
    }

    @Test
    void cannotCancelBookingThatHasStarted() throws Exception {
        long id = bookOk("alice", 1, at(MONDAY, 10, 0), at(MONDAY, 11, 0));

        clock.setNow(MONDAY.atTime(10, 0)); // exactly at start
        cancel("alice", id)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOKING_ALREADY_STARTED"));

        clock.setNow(MONDAY.atTime(11, 30)); // after end
        cancel("alice", id).andExpect(status().isConflict());
    }

    @Test
    void onlyOrganiserOrAdminCanCancel() throws Exception {
        long id = bookOk("alice", 1, at(MONDAY, 10, 0), at(MONDAY, 11, 0));

        cancel("bob", id)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_BOOKING_ORGANISER"));
        cancel("admin", id).andExpect(status().isNoContent());
    }

    @Test
    void returns404ForUnknownBooking() throws Exception {
        cancel("alice", 999)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOOKING_NOT_FOUND"));
    }


    @Test
    void listsActiveBookingsOfRoomForDaySortedByStart() throws Exception {
        bookOk("alice", 1, at(MONDAY, 11, 0), at(MONDAY, 12, 0));
        bookOk("bob", 1, at(MONDAY, 9, 0), at(MONDAY, 10, 0));
        long cancelled = bookOk("bob", 1, at(MONDAY, 14, 0), at(MONDAY, 15, 0));
        cancel("bob", cancelled);
        bookOk("alice", 1, at(MONDAY.plusDays(1), 9, 0), at(MONDAY.plusDays(1), 10, 0)); // other day
        bookOk("alice", 2, at(MONDAY, 9, 0), at(MONDAY, 10, 0));                          // other room

        mvc.perform(get("/api/rooms/1/bookings").param("date", "2026-11-16").with(httpBasic("alice", "alice-pass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].start").value("2026-11-16T09:00:00"))
                .andExpect(jsonPath("$[1].start").value("2026-11-16T11:00:00"));
    }

    @Test
    void validatesRoomBookingsQuery() throws Exception {
        mvc.perform(get("/api/rooms/99/bookings").param("date", "2026-11-16").with(httpBasic("alice", "alice-pass")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/rooms/1/bookings").with(httpBasic("alice", "alice-pass")))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/rooms/1/bookings").param("date", "16/11/2026").with(httpBasic("alice", "alice-pass")))
                .andExpect(status().isBadRequest());
    }


    @Test
    void rejectsMissingFieldsWithFieldErrors() throws Exception {
        mvc.perform(post("/api/bookings").with(httpBasic("alice", "alice-pass"))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"roomId": 1, "end": "2026-11-16T11:00", "subject": "  "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.start").exists())
                .andExpect(jsonPath("$.errors.subject").exists());
    }

    @Test
    void rejectsClientSuppliedOrganiser() throws Exception {
        mvc.perform(post("/api/bookings").with(httpBasic("alice", "alice-pass"))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"roomId": 1, "start": "2026-11-16T10:00", "end": "2026-11-16T11:00",
                                 "subject": "Sync", "organiser": "bob"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsMalformedJsonAndDates() throws Exception {
        mvc.perform(post("/api/bookings").with(httpBasic("alice", "alice-pass"))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"roomId": 1, "start": "tomorrow", "end": "2026-11-16T11:00", "subject": "Sync"}
                                """))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/bookings").with(httpBasic("alice", "alice-pass"))
                        .contentType(APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest());
    }
}