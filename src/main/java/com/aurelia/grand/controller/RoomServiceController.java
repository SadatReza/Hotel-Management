package com.aurelia.grand.controller;

import com.aurelia.grand.model.Booking;
import com.aurelia.grand.model.Room;
import com.aurelia.grand.model.ServiceOrder;
import com.aurelia.grand.model.User;
import com.aurelia.grand.repository.BookingRepository;
import com.aurelia.grand.repository.RoomRepository;
import com.aurelia.grand.repository.ServiceOrderRepository;
import com.aurelia.grand.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
public class RoomServiceController {

    @Autowired
    private ServiceOrderRepository serviceOrderRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/room_service.php")
    public ResponseEntity<?> handleRoomService(
        @RequestParam(value = "action", defaultValue = "place") String action,
        @RequestParam(value = "booking_id", required = false) Long bookingId,
        @RequestParam(value = "items", required = false) String itemsRaw,
        @RequestParam(value = "notes", defaultValue = "") String notes,
        @RequestParam(value = "order_id", required = false) Long orderId,
        @RequestParam(value = "status", required = false) String status,
        HttpSession session
    ) {
        Map<String, Object> response = new HashMap<>();
        Boolean loggedIn = (Boolean) session.getAttribute("logged_in");
        Long sessionUserId = (Long) session.getAttribute("user_id");
        String role = (String) session.getAttribute("role");

        if (loggedIn == null || !loggedIn || sessionUserId == null) {
            response.put("success", false);
            response.put("message", "Not logged in.");
            return ResponseEntity.ok(response);
        }

        User currentUser = userRepository.findById(sessionUserId).get();

        if (action.equals("place")) {
            if (bookingId == null || itemsRaw == null || itemsRaw.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Booking and items are required.");
                return ResponseEntity.ok(response);
            }

            // Verify booking belongs to user
            Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
            if (bookingOpt.isEmpty() || !bookingOpt.get().getUser().getId().equals(sessionUserId)) {
                response.put("success", false);
                response.put("message", "Booking not found.");
                return ResponseEntity.ok(response);
            }

            Booking booking = bookingOpt.get();
            BigDecimal total = BigDecimal.ZERO;
            List<Map<String, Object>> itemsList;
            try {
                itemsList = objectMapper.readValue(itemsRaw, new TypeReference<List<Map<String, Object>>>() {});
                for (Map<String, Object> item : itemsList) {
                    BigDecimal qty = BigDecimal.valueOf(Double.parseDouble(item.get("qty").toString()));
                    BigDecimal price = BigDecimal.valueOf(Double.parseDouble(item.get("price").toString()));
                    total = total.add(qty.multiply(price));
                }
            } catch (Exception e) {
                response.put("success", false);
                response.put("message", "Invalid items format.");
                return ResponseEntity.ok(response);
            }

            ServiceOrder order = new ServiceOrder();
            order.setOrderRef("TMP");
            order.setBooking(booking);
            order.setUser(currentUser);
            order.setRoom(booking.getRoom());
            order.setItems(itemsRaw);
            order.setTotal(total);
            order.setStatus("pending");
            order.setNotes(notes.trim());

            ServiceOrder savedOrder = serviceOrderRepository.save(order);

            String ref = "ORD" + String.format("%04d", savedOrder.getId());
            savedOrder.setOrderRef(ref);
            serviceOrderRepository.save(savedOrder);

            response.put("success", true);
            response.put("order_ref", ref);
            response.put("total", total);
            return ResponseEntity.ok(response);
        }
        else if (action.equals("update_status")) {
            if (role == null || (!role.equals("admin") && !role.equals("staff"))) {
                response.put("success", false);
                response.put("message", "Unauthorized.");
                return ResponseEntity.ok(response);
            }

            List<String> allowed = Arrays.asList("pending", "preparing", "on-the-way", "delivered", "cancelled");
            if (orderId == null || status == null || !allowed.contains(status.trim())) {
                response.put("success", false);
                response.put("message", "Invalid data.");
                return ResponseEntity.ok(response);
            }

            Optional<ServiceOrder> orderOpt = serviceOrderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Order not found.");
                return ResponseEntity.ok(response);
            }

            ServiceOrder order = orderOpt.get();
            order.setStatus(status.trim());
            if (status.trim().equals("preparing")) {
                order.setAssignedTo(currentUser);
            }
            serviceOrderRepository.save(order);

            response.put("success", true);
            return ResponseEntity.ok(response);
        }
        else if (action.equals("get")) {
            List<ServiceOrder> orders;
            if (role.equals("admin") || role.equals("staff")) {
                orders = serviceOrderRepository.findAllByOrderByOrderedAtDesc();
            } else {
                orders = serviceOrderRepository.findByUserOrderByOrderedAtDesc(currentUser);
            }

            List<Map<String, Object>> dataList = new ArrayList<>();
            for (ServiceOrder o : orders) {
                Map<String, Object> oMap = new HashMap<>();
                oMap.put("id", o.getId());
                oMap.put("order_ref", o.getOrderRef());
                oMap.put("booking_id", o.getBooking().getId());
                oMap.put("user_id", o.getUser().getId());
                oMap.put("room_id", o.getRoom().getId());
                oMap.put("items", o.getItems());
                oMap.put("total", o.getTotal());
                oMap.put("status", o.getStatus());
                oMap.put("notes", o.getNotes());
                oMap.put("ordered_at", o.getOrderedAt().toString());

                if (o.getAssignedTo() != null) {
                    oMap.put("assigned_to", o.getAssignedTo().getId());
                } else {
                    oMap.put("assigned_to", null);
                }

                // Add properties expected by frontend: first_name, last_name, room_number, items_array
                oMap.put("first_name", o.getUser().getFirstName());
                oMap.put("last_name", o.getUser().getLastName());
                oMap.put("room_number", o.getRoom().getRoomNumber());

                try {
                    oMap.put("items_array", objectMapper.readValue(o.getItems(), new TypeReference<List<Map<String, Object>>>() {}));
                } catch (Exception e) {
                    oMap.put("items_array", new ArrayList<>());
                }

                dataList.add(oMap);
            }

            response.put("success", true);
            response.put("data", dataList);
            return ResponseEntity.ok(response);
        }

        response.put("success", false);
        response.put("message", "Invalid action.");
        return ResponseEntity.ok(response);
    }
}
