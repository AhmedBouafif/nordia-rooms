package com.nordia.rooms.booking;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class BookingController {

    private final BookingService service;

    public BookingController(BookingService service) {
        this.service = service;
    }

    @PostMapping("/bookings")
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody CreateBookingRequest request,
                                                  Authentication auth) {
        BookingResponse created = service.create(request, auth.getName());
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/bookings/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/bookings/{id}")
    public BookingResponse get(@PathVariable("id") Long id) {
        return service.get(id);
    }

    @DeleteMapping("/bookings/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable("id") Long id, Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        service.cancel(id, auth.getName(), isAdmin);
    }

    @GetMapping("/rooms/{roomId}/bookings")
    public List<BookingResponse> forRoomOnDay(
            @PathVariable("roomId") Long roomId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.listForRoomOnDay(roomId, date);
    }
}