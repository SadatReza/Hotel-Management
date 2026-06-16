package com.aurelia.grand.controller;

import com.aurelia.grand.model.Booking;
import com.aurelia.grand.model.Invoice;
import com.aurelia.grand.model.Room;
import com.aurelia.grand.model.User;
import com.aurelia.grand.repository.BookingRepository;
import com.aurelia.grand.repository.InvoiceRepository;
import com.aurelia.grand.repository.RoomRepository;
import com.aurelia.grand.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
public class BookingController {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @PostMapping("/process_booking.php")
    public ResponseEntity<?> processBooking(
        @RequestParam(value = "room_id", required = false) Long roomId,
        @RequestParam(value = "check_in", required = false) String checkInStr,
        @RequestParam(value = "check_out", required = false) String checkOutStr,
        @RequestParam(value = "guests_count", defaultValue = "1") Integer guestsCount,
        @RequestParam(value = "special_requests", defaultValue = "") String specialRequests,
        HttpSession session
    ) {
        Map<String, Object> response = new HashMap<>();
        Boolean loggedIn = (Boolean) session.getAttribute("logged_in");
        Long userId = (Long) session.getAttribute("user_id");

        if (loggedIn == null || !loggedIn || userId == null) {
            response.put("success", false);
            response.put("message", "You must be logged in to book.");
            return ResponseEntity.ok(response);
        }

        List<String> errors = new ArrayList<>();
        if (roomId == null) errors.add("Room is required.");
        if (checkInStr == null || checkInStr.trim().isEmpty()) errors.add("Check-in date is required.");
        if (checkOutStr == null || checkOutStr.trim().isEmpty()) errors.add("Check-out date is required.");

        if (!errors.isEmpty()) {
            response.put("success", false);
            response.put("errors", errors);
            return ResponseEntity.ok(response);
        }

        LocalDate inDate;
        LocalDate outDate;
        try {
            inDate = LocalDate.parse(checkInStr.trim());
            outDate = LocalDate.parse(checkOutStr.trim());
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Invalid check-in or check-out date format.");
            return ResponseEntity.ok(response);
        }

        LocalDate today = LocalDate.now();
        if (inDate.isBefore(today)) {
            errors.add("Check-in cannot be in the past.");
        }
        if (!outDate.isAfter(inDate)) {
            errors.add("Check-out must be after check-in.");
        }

        if (!errors.isEmpty()) {
            response.put("success", false);
            response.put("errors", errors);
            return ResponseEntity.ok(response);
        }

        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Room not found.");
            return ResponseEntity.ok(response);
        }

        Room room = roomOpt.get();
        if (!room.getStatus().equals("available")) {
            response.put("success", false);
            response.put("message", "Room is currently " + room.getStatus() + ".");
            return ResponseEntity.ok(response);
        }

        // Overlap check
        List<Booking> overlaps = bookingRepository.findOverlappingBookings(roomId, inDate, outDate);
        if (!overlaps.isEmpty()) {
            response.put("success", false);
            response.put("message", "Room is already booked for those dates.");
            return ResponseEntity.ok(response);
        }

        long nights = ChronoUnit.DAYS.between(inDate, outDate);
        BigDecimal total = room.getPrice().multiply(BigDecimal.valueOf(nights));

        User user = userRepository.findById(userId).get();

        Booking booking = new Booking();
        booking.setBookingRef("TMP");
        booking.setUser(user);
        booking.setRoom(room);
        booking.setCheckIn(inDate);
        booking.setCheckOut(outDate);
        booking.setNights((int) nights);
        booking.setGuestsCount(guestsCount);
        booking.setStatus("pending");
        booking.setTotalAmount(total);
        booking.setSpecialRequests(specialRequests.trim());

        Booking savedBooking = bookingRepository.save(booking);

        // Update real booking reference
        String ref = "BK" + String.format("%04d", savedBooking.getId());
        savedBooking.setBookingRef(ref);
        bookingRepository.save(savedBooking);

        // Create Invoice (15% tax)
        BigDecimal tax = total.multiply(BigDecimal.valueOf(0.15)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal invTotal = total.add(tax);
        String invRef = "INV" + String.format("%04d", savedBooking.getId());

        Invoice invoice = new Invoice();
        invoice.setInvoiceRef(invRef);
        invoice.setBooking(savedBooking);
        invoice.setUser(user);
        invoice.setRoomCharges(total);
        invoice.setTaxAmount(tax);
        invoice.setTotalAmount(invTotal);
        invoice.setStatus("issued");
        invoice.setIssuedAt(LocalDateTime.now());
        invoiceRepository.save(invoice);

        response.put("success", true);
        response.put("message", "Booking confirmed!");
        response.put("booking_ref", ref);
        response.put("total", total);
        response.put("nights", nights);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/get_bookings.php")
    public ResponseEntity<?> getBookings(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        Boolean loggedIn = (Boolean) session.getAttribute("logged_in");
        Long userId = (Long) session.getAttribute("user_id");
        String role = (String) session.getAttribute("role");

        if (loggedIn == null || !loggedIn || userId == null) {
            response.put("success", false);
            response.put("message", "Not logged in.");
            return ResponseEntity.ok(response);
        }

        List<Booking> bookings;
        if (role.equals("admin") || role.equals("staff")) {
            bookings = bookingRepository.findAllByOrderByCreatedAtDesc();
        } else {
            User user = userRepository.findById(userId).get();
            bookings = bookingRepository.findByUserOrderByCreatedAtDesc(user);
        }

        List<Map<String, Object>> dataList = new ArrayList<>();
        for (Booking b : bookings) {
            Map<String, Object> bMap = new HashMap<>();
            bMap.put("id", b.getId());
            bMap.put("booking_ref", b.getBookingRef());
            bMap.put("user_id", b.getUser().getId());
            bMap.put("room_id", b.getRoom().getId());
            bMap.put("check_in", b.getCheckIn().toString());
            bMap.put("check_out", b.getCheckOut().toString());
            bMap.put("nights", b.getNights());
            bMap.put("guests_count", b.getGuestsCount());
            bMap.put("status", b.getStatus());
            bMap.put("total_amount", b.getTotalAmount());
            bMap.put("special_requests", b.getSpecialRequests());
            bMap.put("created_at", b.getCreatedAt().toString());

            // Details needed by frontend
            bMap.put("first_name", b.getUser().getFirstName());
            bMap.put("last_name", b.getUser().getLastName());
            bMap.put("user_email", b.getUser().getEmail());
            bMap.put("room_number", b.getRoom().getRoomNumber());
            bMap.put("room_type", b.getRoom().getType());
            bMap.put("floor", b.getRoom().getFloor());
            bMap.put("price", b.getRoom().getPrice());

            dataList.add(bMap);
        }

        response.put("success", true);
        response.put("data", dataList);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/update_booking_status.php")
    public ResponseEntity<?> updateBookingStatus(
        @RequestParam(value = "booking_id", required = false) Long bookingId,
        @RequestParam(value = "status", required = false) String newStatus,
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

        List<String> allowed = Arrays.asList("pending", "approved", "confirmed", "checked_in", "checked_out", "cancelled");
        if (bookingId == null || newStatus == null || !allowed.contains(newStatus.trim())) {
            response.put("success", false);
            response.put("message", "Invalid data.");
            return ResponseEntity.ok(response);
        }

        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
        if (bookingOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Booking not found.");
            return ResponseEntity.ok(response);
        }

        Booking booking = bookingOpt.get();
        booking.setStatus(newStatus.trim());
        bookingRepository.save(booking);

        // Sync room status
        Room room = booking.getRoom();
        if (Arrays.asList("checked_out", "cancelled").contains(newStatus.trim())) {
            room.setStatus("available");
            roomRepository.save(room);
        } else if (newStatus.trim().equals("checked_in")) {
            room.setStatus("occupied");
            roomRepository.save(room);
        }

        response.put("success", true);
        response.put("message", "Status updated to " + newStatus.trim() + ".");

        return ResponseEntity.ok(response);
    }
}
