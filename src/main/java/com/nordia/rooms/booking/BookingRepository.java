package com.nordia.rooms.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
            select count(b) from Booking b
            where b.room.id = :roomId and b.status = :status
              and b.startTime < :end and b.endTime > :start
            """)
    long countOverlapping(@Param("roomId") Long roomId,
                          @Param("start") LocalDateTime start,
                          @Param("end") LocalDateTime end,
                          @Param("status") BookingStatus status);

    @Query("""
            select count(b) from Booking b
            where b.organiser = :organiser and b.status = :status
              and b.startTime >= :dayStart and b.startTime < :dayEnd
            """)
    long countForOrganiserOnDay(@Param("organiser") String organiser,
                                @Param("dayStart") LocalDateTime dayStart,
                                @Param("dayEnd") LocalDateTime dayEnd,
                                @Param("status") BookingStatus status);

    @Query("""
            select b from Booking b
            where b.room.id = :roomId and b.status = :status
              and b.startTime >= :dayStart and b.startTime < :dayEnd
            order by b.startTime
            """)
    List<Booking> findForRoomOnDay(@Param("roomId") Long roomId,
                                   @Param("dayStart") LocalDateTime dayStart,
                                   @Param("dayEnd") LocalDateTime dayEnd,
                                   @Param("status") BookingStatus status);
}