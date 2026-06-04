package com.hotelmanagement.repository;

import com.hotelmanagement.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {}
