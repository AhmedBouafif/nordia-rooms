package com.nordia.rooms.support;

import com.jayway.jsonpath.JsonPath;
import com.nordia.rooms.booking.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestClockConfig.class)
public abstract class ApiTestBase {

    /** Monday 16 November 2026. Tests start at 07:00 that day. */
    protected static final LocalDate MONDAY = LocalDate.of(2026, 11, 16);

    @Autowired protected MockMvc mvc;
    @Autowired protected MutableClock clock;
    @Autowired protected BookingRepository bookings;

    @BeforeEach
    void resetState() {
        bookings.deleteAll();
        clock.setNow(MONDAY.atTime(7, 0));
    }

    protected static String at(LocalDate day, int hour, int minute) {
        return day.atTime(hour, minute).toString();
    }

    protected ResultActions book(String user, long roomId, String start, String end) throws Exception {
        return mvc.perform(post("/api/bookings")
                .with(httpBasic(user, user + "-pass"))
                .contentType(APPLICATION_JSON)
                .content("""
                        {"roomId": %d, "start": "%s", "end": "%s", "subject": "Team sync"}
                        """.formatted(roomId, start, end)));
    }

    protected long bookOk(String user, long roomId, String start, String end) throws Exception {
        String body = book(user, roomId, start, end)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    protected ResultActions cancel(String user, long bookingId) throws Exception {
        return mvc.perform(delete("/api/bookings/{id}", bookingId).with(httpBasic(user, user + "-pass")));
    }
}