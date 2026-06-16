package com.aurelia.grand.controller;

import com.aurelia.grand.model.Booking;
import com.aurelia.grand.model.Review;
import com.aurelia.grand.model.User;
import com.aurelia.grand.repository.BookingRepository;
import com.aurelia.grand.repository.ReviewRepository;
import com.aurelia.grand.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.util.*;

@RestController
public class ReviewController {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @RequestMapping(value = "/reviews.php", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> handleReviews(
        @RequestParam(value = "action", required = false) String action,
        @RequestParam(value = "rating", required = false) Integer rating,
        @RequestParam(value = "comment", required = false) String comment,
        @RequestParam(value = "booking_id", required = false) Long bookingId,
        HttpSession session
    ) {
        Map<String, Object> response = new HashMap<>();

        if (action == null) {
            action = "get";
        }

        if (action.equals("get")) {
            List<Review> list = reviewRepository.findTop50ByIsApprovedOrderByCreatedAtDesc(1);
            List<Map<String, Object>> dataList = new ArrayList<>();
            for (Review r : list) {
                Map<String, Object> rMap = new HashMap<>();
                rMap.put("id", r.getId());
                rMap.put("user_id", r.getUser().getId());
                rMap.put("booking_id", r.getBooking() != null ? r.getBooking().getId() : null);
                rMap.put("rating", r.getRating());
                rMap.put("comment", r.getComment());
                rMap.put("is_approved", r.getIsApproved());
                rMap.put("created_at", r.getCreatedAt().toString());

                // Add fields expected by frontend
                rMap.put("first_name", r.getUser().getFirstName());
                rMap.put("last_name", r.getUser().getLastName());

                dataList.add(rMap);
            }

            response.put("success", true);
            response.put("data", dataList);
            return ResponseEntity.ok(response);
        }
        else if (action.equals("submit")) {
            Boolean loggedIn = (Boolean) session.getAttribute("logged_in");
            Long sessionUserId = (Long) session.getAttribute("user_id");
            String role = (String) session.getAttribute("role");

            if (loggedIn == null || !loggedIn || sessionUserId == null || role == null || !role.equals("guest")) {
                response.put("success", false);
                response.put("message", "Login required.");
                return ResponseEntity.ok(response);
            }

            if (rating == null || rating < 1 || rating > 5 || comment == null || comment.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Rating (1-5) and comment are required.");
                return ResponseEntity.ok(response);
            }

            User currentUser = userRepository.findById(sessionUserId).get();

            Review review = new Review();
            review.setUser(currentUser);
            review.setRating(rating);
            review.setComment(comment.trim());
            review.setIsApproved(1); // Auto-approved in this system

            if (bookingId != null && bookingId > 0) {
                Optional<Booking> bkOpt = bookingRepository.findById(bookingId);
                bkOpt.ifPresent(review::setBooking);
            }

            reviewRepository.save(review);

            response.put("success", true);
            response.put("message", "Review submitted!");
            return ResponseEntity.ok(response);
        }

        response.put("success", false);
        response.put("message", "Invalid action.");
        return ResponseEntity.ok(response);
    }
}
