package com.nordia.rooms.booking;

import com.nordia.rooms.common.LocalDateTimeTextConverter;
import com.nordia.rooms.room.Room;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "bookings", indexes = {
        @Index(name = "idx_booking_room_start", columnList = "room_id, start_time"),
        @Index(name = "idx_booking_organiser_start", columnList = "organiser, start_time")
})
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Convert(converter = LocalDateTimeTextConverter.class)
    @Column(name = "start_time", nullable = false, length = 19)
    private LocalDateTime startTime;

    @Convert(converter = LocalDateTimeTextConverter.class)
    @Column(name = "end_time", nullable = false, length = 19)
    private LocalDateTime endTime;

    @Column(nullable = false, length = 100)
    private String organiser;

    @Column(nullable = false, length = 200)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    @Convert(converter = LocalDateTimeTextConverter.class)
    @Column(name = "created_at", nullable = false, length = 19)
    private LocalDateTime createdAt;

    @Convert(converter = LocalDateTimeTextConverter.class)
    @Column(name = "cancelled_at", length = 19)
    private LocalDateTime cancelledAt;

    protected Booking() {}

    public Booking(Room room, LocalDateTime startTime, LocalDateTime endTime,
                   String organiser, String subject, LocalDateTime createdAt) {
        this.room = room;
        this.startTime = startTime;
        this.endTime = endTime;
        this.organiser = organiser;
        this.subject = subject;
        this.status = BookingStatus.ACTIVE;
        this.createdAt = createdAt;
    }

    void cancel(LocalDateTime at) {
        this.status = BookingStatus.CANCELLED;
        this.cancelledAt = at;
    }

    public Long getId() { return id; }
    public Room getRoom() { return room; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public String getOrganiser() { return organiser; }
    public String getSubject() { return subject; }
    public BookingStatus getStatus() { return status; }
}