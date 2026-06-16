package com.aurelia.grand.repository;

import com.aurelia.grand.model.Booking;
import com.aurelia.grand.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findAllByOrderByCreatedAtDesc();
    List<Booking> findByUserOrderByCreatedAtDesc(User user);

    @Query("SELECT b FROM Booking b WHERE b.room.id = :roomId " +
           "AND b.status NOT IN ('cancelled', 'checked_out') " +
           "AND b.checkIn < :checkout AND b.checkOut > :checkin")
    List<Booking> findOverlappingBookings(
        @Param("roomId") Long roomId,
        @Param("checkin") LocalDate checkin,
        @Param("checkout") LocalDate checkout
    );
}
