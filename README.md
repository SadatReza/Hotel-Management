# 🏨 Aurelia Grand HMS — PHP Backend (XAMPP)

Pure PHP + MySQL backend. Same approach as the reference project.
No Node.js needed. Runs entirely inside XAMPP.

---

## Setup (5 steps)

### 1. Start XAMPP
Open XAMPP Control Panel → Start **Apache** + **MySQL**

### 2. Put the folder in htdocs
Copy this entire folder to:
```
C:\xampp\htdocs\aurelia-grand\
```

### 3. Import the database
- Go to **http://localhost/phpmyadmin**
- Click **New** → name: `aurelia_grand` → collation: `utf8mb4_unicode_ci` → Create
- Click `aurelia_grand` in the sidebar → click **Import** tab
- Choose `schema.sql` → click **Import**

### 4. Create admin password
Open in browser: **http://localhost/aurelia-grand/generate_hash.php**
- Copy the SQL it shows
- Go to phpMyAdmin → `aurelia_grand` → SQL tab → paste and run it
- **Delete generate_hash.php after use**

### 5. Open the site
**http://localhost/aurelia-grand/index.html**

---

## File Map

```
aurelia-grand/
├── index.html              ← Frontend (your original, now wired to PHP)
├── schema.sql              ← Run once in phpMyAdmin
├── generate_hash.php       ← Run once to create admin password, then delete
│
├── includes/
│   └── database.php        ← MySQL connection (edit password here if needed)
│
├── login.php               ← POST: email, password → JSON
├── register.php            ← POST: first_name, last_name, email, password, role → JSON
├── logout.php              ← GET/POST → destroys session
├── get_session.php         ← GET → returns current logged-in user
│
├── get_rooms.php           ← GET ?status=available → room list JSON
├── process_booking.php     ← POST: room_id, check_in, check_out → JSON
├── get_bookings.php        ← GET → bookings for current user (or all if admin/staff)
├── update_booking_status.php ← POST: booking_id, status → JSON
│
├── room_service.php        ← POST action=place/get/update_status → JSON
├── cleaning.php            ← POST action=request/get/update → JSON
├── reviews.php             ← POST/GET action=get/submit → JSON
├── invoices.php            ← GET action=get/mark_paid → JSON
│
├── chat_api.php            ← Guest chat (send_message / fetch_messages)
├── admin_chat_handler.php  ← Admin chat (get_inbox / fetch_conversation / send_reply)
│
└── admin/
    ├── dashboard.php       ← GET → stats JSON (admin only)
    ├── staff.php           ← GET/POST action=get/add/toggle_duty/deactivate
    └── tasks.php           ← GET/POST action=get/add/update_status/delete
```

---

## Default Login

| Role  | Email                        | Password    |
|-------|------------------------------|-------------|
| Admin | admin@aureliagrand.com       | Admin@1234  |

Register new guests/staff through the frontend login form.

---

## How the Frontend Connects

The `index.html` uses `fetch()` to call PHP files:

```javascript
// Login example
const fd = new FormData();
fd.append('email', email);
fd.append('password', password);
const res  = await fetch('login.php', { method: 'POST', body: fd });
const data = await res.json();
// data.success, data.user.name, data.user.role

// Get rooms example
const res  = await fetch('get_rooms.php?status=available');
const data = await res.json();
// data.data → array of room objects

// Book a room
const fd = new FormData();
fd.append('room_id', 3);
fd.append('check_in', '2026-07-10');
fd.append('check_out', '2026-07-14');
const res  = await fetch('process_booking.php', { method: 'POST', body: fd });
const data = await res.json();
// data.booking_ref, data.total, data.nights
```

---

## Troubleshooting

| Problem | Fix |
|---|---|
| Blank page / no JSON | Check Apache & MySQL are running in XAMPP |
| `Connection failed` | Check `includes/database.php` — make sure DB name is `aurelia_grand` |
| Login says invalid | Run `generate_hash.php` to reset the admin password |
| Booking fails | Make sure you're logged in first |
