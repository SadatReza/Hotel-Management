package com.aurelia.grand.controller;

import com.aurelia.grand.model.Room;
import com.aurelia.grand.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.*;

@RestController
public class RoomController {

    @Autowired
    private RoomRepository roomRepository;

    @GetMapping("/get_rooms.php")
    public ResponseEntity<?> getRooms(
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "type", required = false) String type,
        @RequestParam(value = "min_price", required = false) BigDecimal minPrice,
        @RequestParam(value = "max_price", required = false) BigDecimal maxPrice
    ) {
        // Clean status param
        String cleanStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            List<String> allowed = Arrays.asList("available", "occupied", "cleaning", "maintenance");
            if (allowed.contains(status.trim())) {
                cleanStatus = status.trim();
            }
        }

        String cleanType = (type != null && !type.trim().isEmpty()) ? type.trim() : null;

        List<Room> rooms = roomRepository.findRoomsFiltered(cleanStatus, cleanType, minPrice, maxPrice);

        List<Map<String, Object>> dataList = new ArrayList<>();
        for (Room r : rooms) {
            Map<String, Object> rMap = new HashMap<>();
            rMap.put("id", r.getId());
            rMap.put("room_number", r.getRoomNumber());
            rMap.put("type", r.getType());
            rMap.put("floor", r.getFloor());
            rMap.put("status", r.getStatus());
            rMap.put("price", r.getPrice());
            rMap.put("capacity", r.getCapacity());
            rMap.put("amenities", r.getAmenities());
            rMap.put("image_url", r.getImageUrl());
            rMap.put("created_at", r.getCreatedAt().toString());

            // Add amenities_array like PHP does: explode(',', amenities)
            String amenitiesStr = r.getAmenities();
            List<String> amenitiesArray = new ArrayList<>();
            if (amenitiesStr != null && !amenitiesStr.trim().isEmpty()) {
                for (String s : amenitiesStr.split(",")) {
                    amenitiesArray.add(s.trim());
                }
            }
            rMap.put("amenities_array", amenitiesArray);

            dataList.add(rMap);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", dataList);

        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/admin/rooms_api.php", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> adminRoomsApi(
        @RequestParam(value = "action", defaultValue = "get") String action,
        @RequestParam(value = "room_number", required = false) String roomNumber,
        @RequestParam(value = "type", defaultValue = "Deluxe") String type,
        @RequestParam(value = "floor", required = false) Integer floor,
        @RequestParam(value = "price", required = false) BigDecimal price,
        @RequestParam(value = "capacity", required = false) Integer capacity,
        @RequestParam(value = "amenities", required = false) String amenities,
        @RequestParam(value = "id", required = false) Long id,
        @RequestParam(value = "status", required = false) String status,
        HttpSession session
    ) {
        Map<String, Object> response = new HashMap<>();
        Boolean loggedIn = (Boolean) session.getAttribute("logged_in");
        String role = (String) session.getAttribute("role");

        if (loggedIn == null || !loggedIn || role == null || (!role.equals("admin") && !role.equals("staff"))) {
            response.put("success", false);
            response.put("message", "Unauthorized.");
            return ResponseEntity.ok(response);
        }

        if (action.equals("add")) {
            if (!role.equals("admin")) {
                response.put("success", false);
                response.put("message", "Unauthorized.");
                return ResponseEntity.ok(response);
            }

            if (roomNumber == null || roomNumber.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Room number is required.");
                return ResponseEntity.ok(response);
            }

            Optional<Room> existing = roomRepository.findByRoomNumber(roomNumber.trim());
            if (existing.isPresent()) {
                response.put("success", false);
                response.put("message", "Room number already exists.");
                return ResponseEntity.ok(response);
            }

            Room room = new Room();
            room.setRoomNumber(roomNumber.trim());
            room.setType(type);
            room.setFloor(floor != null ? floor : 1);
            room.setPrice(price != null ? price : BigDecimal.valueOf(150));
            room.setCapacity(capacity != null ? capacity : 2);
            room.setAmenities(amenities != null ? amenities.trim() : "");
            room.setStatus("available");

            roomRepository.save(room);

            response.put("success", true);
            response.put("message", "Room added successfully!");
            return ResponseEntity.ok(response);
        }
        else if (action.equals("delete")) {
            if (!role.equals("admin")) {
                response.put("success", false);
                response.put("message", "Unauthorized.");
                return ResponseEntity.ok(response);
            }

            if (id == null) {
                response.put("success", false);
                response.put("message", "Room ID is required.");
                return ResponseEntity.ok(response);
            }

            try {
                roomRepository.deleteById(id);
                response.put("success", true);
                response.put("message", "Room removed.");
            } catch (Exception e) {
                response.put("success", false);
                response.put("message", "Failed to remove room. Maybe it has active bookings.");
            }
            return ResponseEntity.ok(response);
        }
        else if (action.equals("update")) {
            if (id == null || status == null) {
                response.put("success", false);
                response.put("message", "Invalid parameters.");
                return ResponseEntity.ok(response);
            }

            List<String> allowed = Arrays.asList("available", "occupied", "cleaning", "maintenance");
            if (!allowed.contains(status.trim())) {
                response.put("success", false);
                response.put("message", "Invalid parameters.");
                return ResponseEntity.ok(response);
            }

            Optional<Room> roomOpt = roomRepository.findById(id);
            if (roomOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Room not found.");
                return ResponseEntity.ok(response);
            }

            Room room = roomOpt.get();
            room.setStatus(status.trim());
            roomRepository.save(room);

            response.put("success", true);
            response.put("message", "Room updated.");
            return ResponseEntity.ok(response);
        }

        // Default: GET (returns all rooms)
        List<Room> rooms = roomRepository.findRoomsFiltered(null, null, null, null);
        response.put("success", true);
        response.put("data", rooms);
        return ResponseEntity.ok(response);
    }
}
