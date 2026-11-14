package com.nordia.rooms.booking;

import com.nordia.rooms.common.ApiException;
import com.nordia.rooms.common.ErrorCode;
import com.nordia.rooms.support.ApiTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BookingConcurrencyTest extends ApiTestBase {

    @Autowired BookingService service;

    @Test
    void onlyOneOfManySimultaneousRequestsForTheSameSlotSucceeds() throws Exception {
        int threads = 10;
        var request = new CreateBookingRequest(1L, MONDAY.atTime(10, 0), MONDAY.atTime(11, 0), "Race");
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            String organiser = "user" + i;
            results.add(pool.submit(() -> {
                go.await();
                try {
                    service.create(request, organiser);
                    return true;
                } catch (ApiException e) {
                    assertEquals(ErrorCode.ROOM_ALREADY_BOOKED, e.getCode());
                    return false;
                }
            }));
        }
        go.countDown();

        int succeeded = 0;
        for (Future<Boolean> f : results) {
            if (f.get(30, TimeUnit.SECONDS)) succeeded++;
        }
        pool.shutdown();

        assertEquals(1, succeeded);
        assertEquals(1, bookings.count());
    }
}