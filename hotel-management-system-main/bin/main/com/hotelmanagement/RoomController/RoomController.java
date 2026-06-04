package com.hotel.controller;

import com.hotel.model.Room;
import com.hotel.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    @Autowired
    private RoomRepository roomRepository;

    // GET all rooms
    @GetMapping
    public ResponseEntity<List<Room>> getAllRooms() {
        List<Room> rooms = roomRepository.findAll();
        return ResponseEntity.ok(rooms);
    }

    // GET room by ID
    @GetMapping("/{id}")
    public ResponseEntity<Room> getRoomById(@PathVariable Long id) {
        Optional<Room> room = roomRepository.findById(id);
        return room.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    // GET available rooms only
    @GetMapping("/available")
    public ResponseEntity<List<Room>> getAvailableRooms() {
        List<Room> rooms = roomRepository.findAll()
                .stream()
                .filter(Room::isAvailable)
                .toList();
        return ResponseEntity.ok(rooms);
    }

    // POST create room
    @PostMapping
    public ResponseEntity<Room> createRoom(@RequestBody Room room) {
        Room saved = roomRepository.save(room);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // PUT update room
    @PutMapping("/{id}")
    public ResponseEntity<Room> updateRoom(@PathVariable Long id, @RequestBody Room updatedRoom) {
        return roomRepository.findById(id).map(existing -> {
            existing.setRoomType(updatedRoom.getRoomType());
            existing.setPrice(updatedRoom.getPrice());
            existing.setAvailable(updatedRoom.isAvailable());
            Room saved = roomRepository.save(existing);
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    // DELETE room
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        if (!roomRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        roomRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
