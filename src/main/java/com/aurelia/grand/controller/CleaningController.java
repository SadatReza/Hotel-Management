package com.aurelia.grand.controller;

import com.aurelia.grand.model.CleaningRequest;
import com.aurelia.grand.model.Room;
import com.aurelia.grand.model.User;
import com.aurelia.grand.repository.CleaningRequestRepository;
import com.aurelia.grand.repository.RoomRepository;
import com.aurelia.grand.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.*;

@RestController
public class CleaningController {

    @Autowired
    private CleaningRequestRepository cleaningRequestRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    @RequestMapping(value = "/cleaning.php", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> handleCleaning(
        @RequestParam(value = "action", required = false) String action,
        @RequestParam(value = "room_id", required = false) Long roomId,
        @RequestParam(value = "priority", defaultValue = "normal") String priority,
        @RequestParam(value = "notes", defaultValue = "") String notes,
        @RequestParam(value = "id", required = false) Long id,
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

        // Default action is 'get' if not specified (checks both POST and GET)
        if (action == null) {
            action = "get";
        }

        if (action.equals("request")) {
            if (roomId == null) {
                response.put("success", false);
                response.put("message", "Room is required.");
                return ResponseEntity.ok(response);
            }

            Optional<Room> roomOpt = roomRepository.findById(roomId);
            if (roomOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Room not found.");
                return ResponseEntity.ok(response);
            }

            Room room = roomOpt.get();
            List<String> allowed = Arrays.asList("low", "normal", "high", "urgent");
            String cleanPriority = allowed.contains(priority.toLowerCase()) ? priority.toLowerCase() : "normal";

            CleaningRequest request = new CleaningRequest();
            request.setRequestRef("TMP");
            request.setRoom(room);
            request.setRequestedBy(currentUser);
            request.setPriority(cleanPriority);
            request.setNotes(notes.trim());
            request.setStatus("pending");

            CleaningRequest saved = cleaningRequestRepository.save(request);

            String ref = "CR" + String.format("%04d", saved.getId());
            saved.setRequestRef(ref);
            cleaningRequestRepository.save(saved);

            // Update room status to cleaning
            room.setStatus("cleaning");
            roomRepository.save(room);

            response.put("success", true);
            response.put("request_ref", ref);
            return ResponseEntity.ok(response);
        }
        else if (action.equals("update")) {
            if (role == null || (!role.equals("admin") && !role.equals("staff"))) {
                response.put("success", false);
                response.put("message", "Unauthorized.");
                return ResponseEntity.ok(response);
            }

            List<String> allowed = Arrays.asList("pending", "in-progress", "done", "cancelled");
            if (id == null || status == null || !allowed.contains(status.trim())) {
                response.put("success", false);
                return ResponseEntity.ok(response);
            }

            Optional<CleaningRequest> reqOpt = cleaningRequestRepository.findById(id);
            if (reqOpt.isEmpty()) {
                response.put("success", false);
                return ResponseEntity.ok(response);
            }

            CleaningRequest request = reqOpt.get();
            String cleanStatus = status.trim();

            if (cleanStatus.equals("done")) {
                Room room = request.getRoom();
                room.setStatus("available");
                roomRepository.save(room);

                request.setStatus("done");
                request.setCompletedAt(LocalDateTime.now());
                cleaningRequestRepository.save(request);
            } else {
                request.setStatus(cleanStatus);
                if (cleanStatus.equals("in-progress")) {
                    request.setAssignedTo(currentUser);
                }
                cleaningRequestRepository.save(request);
            }

            response.put("success", true);
            return ResponseEntity.ok(response);
        }
        else if (action.equals("get")) {
            List<CleaningRequest> list;
            if (role.equals("admin") || role.equals("staff")) {
                list = cleaningRequestRepository.findAllByOrderByRequestedAtDesc();
            } else {
                list = cleaningRequestRepository.findByRequestedByOrderByRequestedAtDesc(currentUser);
            }

            List<Map<String, Object>> dataList = new ArrayList<>();
            for (CleaningRequest cr : list) {
                Map<String, Object> crMap = new HashMap<>();
                crMap.put("id", cr.getId());
                crMap.put("request_ref", cr.getRequestRef());
                crMap.put("room_id", cr.getRoom().getId());
                crMap.put("requested_by", cr.getRequestedBy().getId());
                crMap.put("priority", cr.getPriority());
                crMap.put("status", cr.getStatus());
                crMap.put("notes", cr.getNotes());
                crMap.put("requested_at", cr.getRequestedAt().toString());

                if (cr.getCompletedAt() != null) {
                    crMap.put("completed_at", cr.getCompletedAt().toString());
                } else {
                    crMap.put("completed_at", null);
                }

                if (cr.getAssignedTo() != null) {
                    crMap.put("assigned_to", cr.getAssignedTo().getId());
                } else {
                    crMap.put("assigned_to", null);
                }

                // Add fields expected by frontend
                crMap.put("room_number", cr.getRoom().getRoomNumber());

                if (role.equals("admin") || role.equals("staff")) {
                    crMap.put("req_first", cr.getRequestedBy().getFirstName());
                    crMap.put("req_last", cr.getRequestedBy().getLastName());
                    if (cr.getAssignedTo() != null) {
                        crMap.put("asgn_first", cr.getAssignedTo().getFirstName());
                        crMap.put("asgn_last", cr.getAssignedTo().getLastName());
                    } else {
                        crMap.put("asgn_first", null);
                        crMap.put("asgn_last", null);
                    }
                }

                dataList.add(crMap);
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
