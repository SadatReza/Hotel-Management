package com.aurelia.grand.controller;

import com.aurelia.grand.model.*;
import com.aurelia.grand.repository.*;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class AdminController {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StaffProfileRepository staffProfileRepository;

    @Autowired
    private ServiceOrderRepository serviceOrderRepository;

    @Autowired
    private AdminTaskRepository adminTaskRepository;

    @GetMapping("/admin/dashboard.php")
    public ResponseEntity<?> getDashboardStats(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        Boolean loggedIn = (Boolean) session.getAttribute("logged_in");
        String role = (String) session.getAttribute("role");

        if (loggedIn == null || !loggedIn || role == null || !role.equals("admin")) {
            response.put("success", false);
            response.put("message", "Unauthorized.");
            return ResponseEntity.ok(response);
        }

        // 1. Room stats
        List<Room> allRooms = roomRepository.findAll();
        long totalRooms = allRooms.size();
        long availableRooms = allRooms.stream().filter(r -> r.getStatus().equals("available")).count();
        long occupiedRooms = allRooms.stream().filter(r -> r.getStatus().equals("occupied")).count();
        long cleaningRooms = allRooms.stream().filter(r -> r.getStatus().equals("cleaning")).count();
        long maintenanceRooms = allRooms.stream().filter(r -> r.getStatus().equals("maintenance")).count();

        Map<String, Object> roomStats = new HashMap<>();
        roomStats.put("total", totalRooms);
        roomStats.put("available", availableRooms);
        roomStats.put("occupied", occupiedRooms);
        roomStats.put("cleaning", cleaningRooms);
        roomStats.put("maintenance", maintenanceRooms);

        // 2. Booking stats
        List<Booking> allBookings = bookingRepository.findAll();
        List<Booking> activeBookings = allBookings.stream()
                .filter(b -> !b.getStatus().equals("cancelled"))
                .collect(Collectors.toList());

        long totalBookings = activeBookings.size();
        long confirmedBookings = activeBookings.stream().filter(b -> b.getStatus().equals("confirmed")).count();
        long approvedBookings = activeBookings.stream().filter(b -> b.getStatus().equals("approved")).count();
        long pendingBookings = activeBookings.stream().filter(b -> b.getStatus().equals("pending")).count();
        BigDecimal totalRevenue = activeBookings.stream()
                .map(Booking::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> bookingStats = new HashMap<>();
        bookingStats.put("total", totalBookings);
        bookingStats.put("confirmed", confirmedBookings);
        bookingStats.put("approved", approvedBookings);
        bookingStats.put("pending", pendingBookings);
        bookingStats.put("total_revenue", totalRevenue);

        // 3. Staff stats
        List<User> staffUsers = userRepository.findByRoleAndIsActive("staff", 1);
        long totalStaff = staffUsers.size();
        long onDutyStaff = 0;
        for (User u : staffUsers) {
            Optional<StaffProfile> spOpt = staffProfileRepository.findByUserId(u.getId());
            if (spOpt.isPresent() && spOpt.get().getOnDuty() == 1) {
                onDutyStaff++;
            }
        }

        Map<String, Object> staffStats = new HashMap<>();
        staffStats.put("total", totalStaff);
        staffStats.put("on_duty", onDutyStaff);

        // 4. Recent Bookings (limit 5)
        List<Booking> recentList = bookingRepository.findAllByOrderByCreatedAtDesc();
        List<Map<String, Object>> recentBookings = new ArrayList<>();
        int count = 0;
        for (Booking b : recentList) {
            if (count >= 5) break;
            Map<String, Object> bMap = new HashMap<>();
            bMap.put("booking_ref", b.getBookingRef());
            bMap.put("check_in", b.getCheckIn().toString());
            bMap.put("check_out", b.getCheckOut().toString());
            bMap.put("total_amount", b.getTotalAmount());
            bMap.put("status", b.getStatus());
            bMap.put("first_name", b.getUser().getFirstName());
            bMap.put("last_name", b.getUser().getLastName());
            bMap.put("room_number", b.getRoom().getRoomNumber());
            recentBookings.add(bMap);
            count++;
        }

        // 5. Pending Service Orders (limit 5)
        List<ServiceOrder> orderList = serviceOrderRepository.findByStatusIn(Arrays.asList("pending", "preparing"));
        orderList.sort((o1, o2) -> o2.getOrderedAt().compareTo(o1.getOrderedAt())); // desc order
        List<Map<String, Object>> pendingOrders = new ArrayList<>();
        count = 0;
        for (ServiceOrder o : orderList) {
            if (count >= 5) break;
            Map<String, Object> oMap = new HashMap<>();
            oMap.put("order_ref", o.getOrderRef());
            oMap.put("total", o.getTotal());
            oMap.put("status", o.getStatus());
            oMap.put("first_name", o.getUser().getFirstName());
            oMap.put("last_name", o.getUser().getLastName());
            oMap.put("room_number", o.getRoom().getRoomNumber());
            pendingOrders.add(oMap);
            count++;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("room_stats", roomStats);
        data.put("booking_stats", bookingStats);
        data.put("staff_stats", staffStats);
        data.put("recent_bookings", recentBookings);
        data.put("pending_orders", pendingOrders);

        response.put("success", true);
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/admin/staff.php", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> handleStaff(
        @RequestParam(value = "action", defaultValue = "get") String action,
        @RequestParam(value = "first_name", required = false) String firstName,
        @RequestParam(value = "last_name", required = false) String lastName,
        @RequestParam(value = "email", required = false) String email,
        @RequestParam(value = "password", defaultValue = "Staff@1234") String password,
        @RequestParam(value = "department", required = false) String department,
        @RequestParam(value = "zone", required = false) String zone,
        @RequestParam(value = "shift_start", required = false) String shiftStart,
        @RequestParam(value = "shift_end", required = false) String shiftEnd,
        @RequestParam(value = "user_id", required = false) Long targetUserId,
        @RequestParam(value = "on_duty", required = false) Integer onDuty,
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

        if (action.equals("get")) {
            List<User> staffUsers = userRepository.findByRoleAndIsActive("staff", 1);
            staffUsers.sort(Comparator.comparing(User::getFirstName));

            List<Map<String, Object>> dataList = new ArrayList<>();
            for (User u : staffUsers) {
                Map<String, Object> sMap = new HashMap<>();
                sMap.put("id", u.getId());
                sMap.put("first_name", u.getFirstName());
                sMap.put("last_name", u.getLastName());
                sMap.put("email", u.getEmail());
                sMap.put("phone", u.getPhone());
                sMap.put("is_active", u.getIsActive());

                Optional<StaffProfile> spOpt = staffProfileRepository.findByUserId(u.getId());
                if (spOpt.isPresent()) {
                    StaffProfile sp = spOpt.get();
                    sMap.put("department", sp.getDepartment());
                    sMap.put("zone", sp.getZone());
                    sMap.put("shift_start", sp.getShiftStart().toString());
                    sMap.put("shift_end", sp.getShiftEnd().toString());
                    sMap.put("on_duty", sp.getOnDuty());
                } else {
                    sMap.put("department", "Housekeeper");
                    sMap.put("zone", "All Floors");
                    sMap.put("shift_start", "08:00:00");
                    sMap.put("shift_end", "16:00:00");
                    sMap.put("on_duty", 1);
                }
                dataList.add(sMap);
            }

            response.put("success", true);
            response.put("data", dataList);
            return ResponseEntity.ok(response);
        }
        else if (action.equals("add")) {
            if (!role.equals("admin")) {
                response.put("success", false);
                response.put("message", "Unauthorized.");
                return ResponseEntity.ok(response);
            }

            if (firstName == null || lastName == null || email == null || firstName.trim().isEmpty() || lastName.trim().isEmpty() || email.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "First name, last name, and email are required.");
                return ResponseEntity.ok(response);
            }

            Optional<User> checkUser = userRepository.findByEmail(email.trim());
            if (checkUser.isPresent()) {
                response.put("success", false);
                response.put("message", "Email already exists.");
                return ResponseEntity.ok(response);
            }

            String hash = BCrypt.hashpw(password, BCrypt.gensalt());
            User user = new User();
            user.setFirstName(firstName.trim());
            user.setLastName(lastName.trim());
            user.setEmail(email.trim());
            user.setPassword(hash);
            user.setRole("staff");
            user.setIsActive(1);
            User saved = userRepository.save(user);

            StaffProfile profile = new StaffProfile();
            profile.setUser(saved);
            profile.setDepartment(department != null ? department.trim() : "Housekeeper");
            profile.setZone(zone != null ? zone.trim() : "All Floors");
            profile.setShiftStart(parseTime(shiftStart, "08:00"));
            profile.setShiftEnd(parseTime(shiftEnd, "16:00"));
            profile.setOnDuty(1);
            staffProfileRepository.save(profile);

            response.put("success", true);
            response.put("message", "Staff added!");
            return ResponseEntity.ok(response);
        }
        else if (action.equals("update")) {
            if (!role.equals("admin")) {
                response.put("success", false);
                response.put("message", "Unauthorized.");
                return ResponseEntity.ok(response);
            }

            if (targetUserId == null || department == null || zone == null || department.trim().isEmpty() || zone.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Invalid parameters.");
                return ResponseEntity.ok(response);
            }

            Optional<User> uOpt = userRepository.findById(targetUserId);
            if (uOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Staff user not found.");
                return ResponseEntity.ok(response);
            }

            User user = uOpt.get();
            Optional<StaffProfile> spOpt = staffProfileRepository.findByUserId(targetUserId);
            StaffProfile profile = spOpt.orElseGet(() -> {
                StaffProfile sp = new StaffProfile();
                sp.setUser(user);
                return sp;
            });

            profile.setDepartment(department.trim());
            profile.setZone(zone.trim());
            profile.setShiftStart(parseTime(shiftStart, "08:00"));
            profile.setShiftEnd(parseTime(shiftEnd, "16:00"));

            staffProfileRepository.save(profile);

            response.put("success", true);
            response.put("message", "Staff profile updated.");
            return ResponseEntity.ok(response);
        }
        else if (action.equals("toggle_duty")) {
            if (targetUserId == null || onDuty == null) {
                response.put("success", false);
                return ResponseEntity.ok(response);
            }

            Optional<User> uOpt = userRepository.findById(targetUserId);
            if (uOpt.isEmpty()) {
                response.put("success", false);
                return ResponseEntity.ok(response);
            }

            User user = uOpt.get();
            Optional<StaffProfile> spOpt = staffProfileRepository.findByUserId(targetUserId);
            StaffProfile profile = spOpt.orElseGet(() -> {
                StaffProfile sp = new StaffProfile();
                sp.setUser(user);
                sp.setDepartment("Housekeeper");
                sp.setZone("All Floors");
                sp.setShiftStart(LocalTime.of(8, 0));
                sp.setShiftEnd(LocalTime.of(16, 0));
                return sp;
            });

            profile.setOnDuty(onDuty);
            staffProfileRepository.save(profile);

            response.put("success", true);
            return ResponseEntity.ok(response);
        }
        else if (action.equals("deactivate")) {
            if (!role.equals("admin")) {
                response.put("success", false);
                response.put("message", "Unauthorized.");
                return ResponseEntity.ok(response);
            }

            if (targetUserId == null) {
                response.put("success", false);
                return ResponseEntity.ok(response);
            }

            Optional<User> uOpt = userRepository.findById(targetUserId);
            if (uOpt.isPresent() && uOpt.get().getRole().equals("staff")) {
                User u = uOpt.get();
                u.setIsActive(0);
                userRepository.save(u);
            }

            response.put("success", true);
            return ResponseEntity.ok(response);
        }

        response.put("success", false);
        response.put("message", "Invalid action.");
        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/admin/tasks.php", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> handleTasks(
        @RequestParam(value = "action", defaultValue = "get") String action,
        @RequestParam(value = "title", required = false) String title,
        @RequestParam(value = "description", required = false) String description,
        @RequestParam(value = "assigned_to", required = false) Long assignedToUserId,
        @RequestParam(value = "priority", defaultValue = "medium") String priority,
        @RequestParam(value = "due_date", required = false) String dueDateStr,
        @RequestParam(value = "id", required = false) Long taskId,
        @RequestParam(value = "status", required = false) String status,
        HttpSession session
    ) {
        Map<String, Object> response = new HashMap<>();
        Boolean loggedIn = (Boolean) session.getAttribute("logged_in");
        Long sessionUserId = (Long) session.getAttribute("user_id");
        String role = (String) session.getAttribute("role");

        if (loggedIn == null || !loggedIn || sessionUserId == null || role == null || (!role.equals("admin") && !role.equals("staff"))) {
            response.put("success", false);
            response.put("message", "Unauthorized.");
            return ResponseEntity.ok(response);
        }

        User currentUser = userRepository.findById(sessionUserId).get();

        if (action.equals("get")) {
            List<AdminTask> tasks;
            if (role.equals("admin")) {
                tasks = adminTaskRepository.findAllByOrderByCreatedAtDesc();
            } else {
                tasks = adminTaskRepository.findByAssignedToOrderByCreatedAtDesc(currentUser);
            }

            List<Map<String, Object>> dataList = new ArrayList<>();
            for (AdminTask t : tasks) {
                Map<String, Object> tMap = new HashMap<>();
                tMap.put("id", t.getId());
                tMap.put("title", t.getTitle());
                tMap.put("description", t.getDescription());
                tMap.put("priority", t.getPriority());
                tMap.put("status", t.getStatus());
                tMap.put("due_date", t.getDueDate() != null ? t.getDueDate().toString() : null);
                tMap.put("created_at", t.getCreatedAt().toString());

                if (t.getAssignedTo() != null) {
                    tMap.put("assigned_to", t.getAssignedTo().getId());
                    tMap.put("first_name", t.getAssignedTo().getFirstName());
                    tMap.put("last_name", t.getAssignedTo().getLastName());
                } else {
                    tMap.put("assigned_to", null);
                    tMap.put("first_name", null);
                    tMap.put("last_name", null);
                }

                dataList.add(tMap);
            }

            response.put("success", true);
            response.put("data", dataList);
            return ResponseEntity.ok(response);
        }
        else if (action.equals("add")) {
            if (!role.equals("admin")) {
                response.put("success", false);
                response.put("message", "Unauthorized.");
                return ResponseEntity.ok(response);
            }

            if (title == null || title.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Title is required.");
                return ResponseEntity.ok(response);
            }

            AdminTask task = new AdminTask();
            task.setTitle(title.trim());
            task.setDescription(description != null ? description.trim() : "");
            task.setCreatedBy(currentUser);
            task.setPriority(priority);
            task.setStatus("todo");

            if (assignedToUserId != null && assignedToUserId > 0) {
                Optional<User> staffOpt = userRepository.findById(assignedToUserId);
                staffOpt.ifPresent(task::setAssignedTo);
            }

            if (dueDateStr != null && !dueDateStr.trim().isEmpty()) {
                try {
                    task.setDueDate(LocalDate.parse(dueDateStr.trim()));
                } catch (Exception e) {
                    // Ignore invalid format
                }
            }

            adminTaskRepository.save(task);

            response.put("success", true);
            return ResponseEntity.ok(response);
        }
        else if (action.equals("update_status")) {
            List<String> allowed = Arrays.asList("todo", "in-progress", "done", "cancelled");
            if (taskId == null || status == null || !allowed.contains(status.trim())) {
                response.put("success", false);
                return ResponseEntity.ok(response);
            }

            Optional<AdminTask> taskOpt = adminTaskRepository.findById(taskId);
            if (taskOpt.isEmpty()) {
                response.put("success", false);
                return ResponseEntity.ok(response);
            }

            AdminTask task = taskOpt.get();
            task.setStatus(status.trim());
            adminTaskRepository.save(task);

            response.put("success", true);
            return ResponseEntity.ok(response);
        }
        else if (action.equals("delete")) {
            if (!role.equals("admin")) {
                response.put("success", false);
                response.put("message", "Unauthorized.");
                return ResponseEntity.ok(response);
            }

            if (taskId == null) {
                response.put("success", false);
                return ResponseEntity.ok(response);
            }

            adminTaskRepository.deleteById(taskId);
            response.put("success", true);
            return ResponseEntity.ok(response);
        }

        response.put("success", false);
        response.put("message", "Invalid action.");
        return ResponseEntity.ok(response);
    }

    private LocalTime parseTime(String timeStr, String defaultTime) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            timeStr = defaultTime;
        }
        String clean = timeStr.trim();
        if (clean.length() == 5) {
            clean += ":00";
        }
        try {
            return LocalTime.parse(clean);
        } catch (Exception e) {
            return LocalTime.parse(defaultTime + ":00");
        }
    }
}
