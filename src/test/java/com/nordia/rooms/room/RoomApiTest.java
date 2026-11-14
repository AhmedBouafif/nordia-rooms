package com.nordia.rooms.room;

import com.nordia.rooms.support.ApiTestBase;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoomApiTest extends ApiTestBase {

    @Test
    void listsAllSeededRooms() throws Exception {
        mvc.perform(get("/api/rooms").with(httpBasic("alice", "alice-pass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(6))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.content[0].name").value("Aurora"))
                .andExpect(jsonPath("$.content[0].equipment[0]").value("whiteboard"));
    }

    @Test
    void filtersByMinimumCapacityInclusive() throws Exception {
        mvc.perform(get("/api/rooms").param("minCapacity", "10").with(httpBasic("alice", "alice-pass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name").value(contains("Delta", "Equinox", "Fjord")));
    }

    @Test
    void paginates() throws Exception {
        mvc.perform(get("/api/rooms").param("size", "2").param("page", "1").with(httpBasic("alice", "alice-pass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name").value(contains("Cirrus", "Delta")))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void rejectsInvalidQueryParameters() throws Exception {
        mvc.perform(get("/api/rooms").param("size", "0").with(httpBasic("alice", "alice-pass")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(get("/api/rooms").param("size", "101").with(httpBasic("alice", "alice-pass")))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/rooms").param("minCapacity", "abc").with(httpBasic("alice", "alice-pass")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(get("/api/rooms"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mvc.perform(get("/api/rooms").with(httpBasic("alice", "wrong")))
                .andExpect(status().isUnauthorized());
    }
}