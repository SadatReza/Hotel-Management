package com.aurelia.grand.controller;

import com.aurelia.grand.model.ChatMessage;
import com.aurelia.grand.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.*;

@RestController
public class ChatController {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    @PostMapping("/chat_api.php")
    public ResponseEntity<?> handleGuestChat(
        @RequestParam(value = "action", defaultValue = "") String action,
        @RequestParam(value = "message", defaultValue = "") String message,
        @RequestParam(value = "name", required = false) String customName,
        @RequestParam(value = "email", required = false) String customEmail,
        HttpSession session
    ) {
        Map<String, Object> response = new HashMap<>();

        // Identify current session user or assign guest info
        String currentEmail;
        String currentName;

        Boolean loggedIn = (Boolean) session.getAttribute("logged_in");
        if (loggedIn != null && loggedIn) {
            currentEmail = (String) session.getAttribute("email");
            currentName = (String) session.getAttribute("user_name");
        } else {
            if (session.getAttribute("guest_chat_email") == null) {
                String shortId = session.getId();
                if (shortId.length() > 8) {
                    shortId = shortId.substring(0, 8);
                }
                session.setAttribute("guest_chat_email", "guest_" + shortId + "@guest.local");
                session.setAttribute("guest_chat_name", "Guest User");
            }
            currentEmail = (String) session.getAttribute("guest_chat_email");
            currentName = (String) session.getAttribute("guest_chat_name");
        }

        if (action.equals("send_message")) {
            if (message.trim().isEmpty()) {
                response.put("success", false);
                return ResponseEntity.ok(response);
            }

            if (customName != null && !customName.trim().isEmpty()) {
                currentName = customName.trim();
            }
            if (customEmail != null && !customEmail.trim().isEmpty()) {
                currentEmail = customEmail.trim();
            }

            ChatMessage msg = new ChatMessage();
            msg.setUserName(currentName);
            msg.setUserEmail(currentEmail);
            msg.setMessage(message.trim());
            msg.setMessageType("customer");
            msg.setIsRead(0);
            msg.setCreatedAt(LocalDateTime.now());

            chatMessageRepository.save(msg);

            response.put("success", true);
            return ResponseEntity.ok(response);
        }
        else if (action.equals("fetch_messages")) {
            List<ChatMessage> list = chatMessageRepository.findByUserEmailOrderByCreatedAtAsc(currentEmail);
            List<Map<String, Object>> messagesList = new ArrayList<>();
            for (ChatMessage m : list) {
                Map<String, Object> mMap = new HashMap<>();
                mMap.put("type", m.getMessageType());
                mMap.put("message", m.getMessage());
                mMap.put("time", m.getCreatedAt().format(timeFormatter));
                messagesList.add(mMap);
            }

            response.put("success", true);
            response.put("messages", messagesList);
            return ResponseEntity.ok(response);
        }

        response.put("success", false);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin_chat_handler.php")
    public ResponseEntity<?> handleAdminChat(
        @RequestParam(value = "action", defaultValue = "") String action,
        @RequestParam(value = "email", required = false) String targetEmail,
        @RequestParam(value = "user_name", required = false) String targetName,
        @RequestParam(value = "message", required = false) String message,
        HttpSession session
    ) {
        Map<String, Object> response = new HashMap<>();
        Boolean loggedIn = (Boolean) session.getAttribute("logged_in");
        String role = (String) session.getAttribute("role");

        if (loggedIn == null || !loggedIn || role == null || !role.equals("admin")) {
            response.put("success", false);
            response.put("error", "Unauthorized");
            return ResponseEntity.ok(response);
        }

        if (action.equals("get_inbox")) {
            List<Object[]> inboxListRaw = chatMessageRepository.findInboxList();
            List<Map<String, Object>> usersList = new ArrayList<>();
            for (Object[] row : inboxListRaw) {
                Map<String, Object> uMap = new HashMap<>();
                uMap.put("user_email", row[0]);
                uMap.put("user_name", row[1]);
                uMap.put("last_message", row[2] != null ? row[2].toString() : null);

                // JPA SUM returns Long or BigDecimal or Double. Handle safely.
                Number unreadVal = (Number) row[3];
                uMap.put("unread", unreadVal != null ? unreadVal.longValue() : 0L);

                usersList.add(uMap);
            }

            response.put("success", true);
            response.put("users", usersList);
            return ResponseEntity.ok(response);
        }
        else if (action.equals("fetch_conversation")) {
            if (targetEmail == null || targetEmail.trim().isEmpty()) {
                response.put("success", false);
                return ResponseEntity.ok(response);
            }

            // Mark as read
            chatMessageRepository.markAsReadForEmail(targetEmail.trim());

            // Fetch messages
            List<ChatMessage> list = chatMessageRepository.findByUserEmailOrderByCreatedAtAsc(targetEmail.trim());
            List<Map<String, Object>> messagesList = new ArrayList<>();
            for (ChatMessage m : list) {
                Map<String, Object> mMap = new HashMap<>();
                mMap.put("id", m.getId());
                mMap.put("user_email", m.getUserEmail());
                mMap.put("user_name", m.getUserName());
                mMap.put("message", m.getMessage());
                mMap.put("message_type", m.getMessageType());
                mMap.put("is_read", m.getIsRead());
                mMap.put("created_at", m.getCreatedAt().toString());
                messagesList.add(mMap);
            }

            response.put("success", true);
            response.put("messages", messagesList);
            return ResponseEntity.ok(response);
        }
        else if (action.equals("send_reply")) {
            if (targetEmail == null || targetEmail.trim().isEmpty() || message == null || message.trim().isEmpty()) {
                response.put("success", false);
                return ResponseEntity.ok(response);
            }

            ChatMessage msg = new ChatMessage();
            msg.setUserEmail(targetEmail.trim());
            msg.setUserName(targetName != null ? targetName.trim() : "User");
            msg.setMessage(message.trim());
            msg.setMessageType("admin");
            msg.setIsRead(1);
            msg.setCreatedAt(LocalDateTime.now());

            chatMessageRepository.save(msg);

            response.put("success", true);
            return ResponseEntity.ok(response);
        }

        response.put("success", false);
        return ResponseEntity.ok(response);
    }
}
