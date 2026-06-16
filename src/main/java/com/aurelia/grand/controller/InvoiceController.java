package com.aurelia.grand.controller;

import com.aurelia.grand.model.Invoice;
import com.aurelia.grand.model.User;
import com.aurelia.grand.repository.InvoiceRepository;
import com.aurelia.grand.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.*;

@RestController
public class InvoiceController {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/invoices.php")
    public ResponseEntity<?> handleInvoices(
        @RequestParam(value = "action", defaultValue = "get") String action,
        @RequestParam(value = "id", required = false) Long invoiceId,
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

        if (action.equals("get")) {
            List<Invoice> list;
            if (role.equals("admin") || role.equals("staff")) {
                list = invoiceRepository.findAllByOrderByIssuedAtDesc();
            } else {
                list = invoiceRepository.findByUserOrderByIssuedAtDesc(currentUser);
            }

            List<Map<String, Object>> dataList = new ArrayList<>();
            for (Invoice inv : list) {
                Map<String, Object> iMap = new HashMap<>();
                iMap.put("id", inv.getId());
                iMap.put("invoice_ref", inv.getInvoiceRef());
                iMap.put("booking_id", inv.getBooking().getId());
                iMap.put("user_id", inv.getUser().getId());
                iMap.put("room_charges", inv.getRoomCharges());
                iMap.put("service_charges", inv.getServiceCharges());
                iMap.put("tax_amount", inv.getTaxAmount());
                iMap.put("total_amount", inv.getTotalAmount());
                iMap.put("status", inv.getStatus());
                iMap.put("issued_at", inv.getIssuedAt().toString());

                if (inv.getPaidAt() != null) {
                    iMap.put("paid_at", inv.getPaidAt().toString());
                } else {
                    iMap.put("paid_at", null);
                }

                // Add fields expected by frontend
                iMap.put("booking_ref", inv.getBooking().getBookingRef());
                iMap.put("check_in", inv.getBooking().getCheckIn().toString());
                iMap.put("check_out", inv.getBooking().getCheckOut().toString());
                iMap.put("nights", inv.getBooking().getNights());
                iMap.put("room_number", inv.getBooking().getRoom().getRoomNumber());

                if (role.equals("admin") || role.equals("staff")) {
                    iMap.put("first_name", inv.getUser().getFirstName());
                    iMap.put("last_name", inv.getUser().getLastName());
                    iMap.put("user_email", inv.getUser().getEmail());
                } else {
                    iMap.put("room_type", inv.getBooking().getRoom().getType());
                }

                dataList.add(iMap);
            }

            response.put("success", true);
            response.put("data", dataList);
            return ResponseEntity.ok(response);
        }
        else if (action.equals("mark_paid")) {
            if (role == null || !role.equals("admin")) {
                response.put("success", false);
                response.put("message", "Unauthorized.");
                return ResponseEntity.ok(response);
            }

            if (invoiceId == null) {
                response.put("success", false);
                response.put("message", "Invoice ID required.");
                return ResponseEntity.ok(response);
            }

            Optional<Invoice> invOpt = invoiceRepository.findById(invoiceId);
            if (invOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Invoice not found.");
                return ResponseEntity.ok(response);
            }

            Invoice invoice = invOpt.get();
            invoice.setStatus("paid");
            invoice.setPaidAt(LocalDateTime.now());
            invoiceRepository.save(invoice);

            response.put("success", true);
            return ResponseEntity.ok(response);
        }

        response.put("success", false);
        response.put("message", "Invalid action.");
        return ResponseEntity.ok(response);
    }
}
