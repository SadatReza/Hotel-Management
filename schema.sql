-- ═══════════════════════════════════════════════
-- AURELIA GRAND HMS — MySQL Schema
-- Import this in phpMyAdmin → Import tab
-- ═══════════════════════════════════════════════

CREATE DATABASE IF NOT EXISTS aurelia_grand
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE aurelia_grand;

CREATE TABLE IF NOT EXISTS users (
  id         INT AUTO_INCREMENT PRIMARY KEY,
  first_name VARCHAR(60)  NOT NULL,
  last_name  VARCHAR(60)  NOT NULL,
  email      VARCHAR(120) NOT NULL UNIQUE,
  password   VARCHAR(255) NOT NULL,
  role       ENUM('guest','staff','admin') NOT NULL DEFAULT 'guest',
  phone      VARCHAR(30)  NULL,
  is_active  TINYINT(1)   NOT NULL DEFAULT 1,
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS rooms (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  room_number VARCHAR(10)   NOT NULL UNIQUE,
  type        ENUM('Deluxe','Superior','Junior Suite','Grand Suite','Presidential') NOT NULL,
  floor       TINYINT       NOT NULL,
  status      ENUM('available','occupied','cleaning','maintenance') NOT NULL DEFAULT 'available',
  price       DECIMAL(10,2) NOT NULL,
  capacity    TINYINT       NOT NULL DEFAULT 2,
  amenities   VARCHAR(500)  NULL,
  image_url   VARCHAR(500)  NULL,
  created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS bookings (
  id               INT AUTO_INCREMENT PRIMARY KEY,
  booking_ref      VARCHAR(20)   NOT NULL UNIQUE,
  user_id          INT           NOT NULL,
  room_id          INT           NOT NULL,
  check_in         DATE          NOT NULL,
  check_out        DATE          NOT NULL,
  nights           TINYINT       NOT NULL,
  guests_count     TINYINT       NOT NULL DEFAULT 1,
  status           ENUM('pending','approved','confirmed','checked_in','checked_out','cancelled') NOT NULL DEFAULT 'pending',
  total_amount     DECIMAL(10,2) NOT NULL,
  special_requests TEXT          NULL,
  created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (room_id) REFERENCES rooms(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS staff_profiles (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  user_id     INT         NOT NULL UNIQUE,
  department  ENUM('Housekeeper','Room Service','Receptionist','Maintenance','Chef','Concierge') NOT NULL,
  zone        VARCHAR(60) NOT NULL DEFAULT 'All Floors',
  shift_start TIME        NOT NULL,
  shift_end   TIME        NOT NULL,
  on_duty     TINYINT(1)  NOT NULL DEFAULT 1,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS service_orders (
  id         INT AUTO_INCREMENT PRIMARY KEY,
  order_ref  VARCHAR(20)   NOT NULL UNIQUE,
  booking_id INT           NOT NULL,
  user_id    INT           NOT NULL,
  room_id    INT           NOT NULL,
  items      TEXT          NOT NULL,
  total      DECIMAL(10,2) NOT NULL,
  status     ENUM('pending','preparing','on-the-way','delivered','cancelled') NOT NULL DEFAULT 'pending',
  notes      TEXT          NULL,
  assigned_to INT          NULL,
  ordered_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (booking_id) REFERENCES bookings(id),
  FOREIGN KEY (user_id)    REFERENCES users(id),
  FOREIGN KEY (room_id)    REFERENCES rooms(id),
  FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS cleaning_requests (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  request_ref  VARCHAR(20) NOT NULL UNIQUE,
  room_id      INT         NOT NULL,
  requested_by INT         NOT NULL,
  assigned_to  INT         NULL,
  priority     ENUM('low','normal','high','urgent') NOT NULL DEFAULT 'normal',
  status       ENUM('pending','in-progress','done','cancelled') NOT NULL DEFAULT 'pending',
  notes        TEXT        NULL,
  requested_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_at DATETIME    NULL,
  FOREIGN KEY (room_id)      REFERENCES rooms(id),
  FOREIGN KEY (requested_by) REFERENCES users(id),
  FOREIGN KEY (assigned_to)  REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS chat_messages (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  user_email   VARCHAR(120) NOT NULL,
  user_name    VARCHAR(120) NOT NULL,
  message      TEXT         NOT NULL,
  message_type ENUM('customer','admin') NOT NULL DEFAULT 'customer',
  is_read      TINYINT(1)   NOT NULL DEFAULT 0,
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_email (user_email)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS reviews (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  user_id     INT      NOT NULL,
  booking_id  INT      NULL,
  rating      TINYINT  NOT NULL,
  comment     TEXT     NOT NULL,
  is_approved TINYINT(1) NOT NULL DEFAULT 1,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id)    REFERENCES users(id),
  FOREIGN KEY (booking_id) REFERENCES bookings(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS invoices (
  id              INT AUTO_INCREMENT PRIMARY KEY,
  invoice_ref     VARCHAR(20)   NOT NULL UNIQUE,
  booking_id      INT           NOT NULL UNIQUE,
  user_id         INT           NOT NULL,
  room_charges    DECIMAL(10,2) NOT NULL,
  service_charges DECIMAL(10,2) NOT NULL DEFAULT 0,
  tax_amount      DECIMAL(10,2) NOT NULL DEFAULT 0,
  total_amount    DECIMAL(10,2) NOT NULL,
  status          ENUM('issued','paid','overdue') NOT NULL DEFAULT 'issued',
  issued_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  paid_at         DATETIME      NULL,
  FOREIGN KEY (booking_id) REFERENCES bookings(id),
  FOREIGN KEY (user_id)    REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS admin_tasks (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  title       VARCHAR(200) NOT NULL,
  description TEXT         NULL,
  assigned_to INT          NULL,
  created_by  INT          NOT NULL,
  priority    ENUM('low','medium','high','urgent') NOT NULL DEFAULT 'medium',
  status      ENUM('todo','in-progress','done','cancelled') NOT NULL DEFAULT 'todo',
  due_date    DATE         NULL,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (assigned_to) REFERENCES users(id),
  FOREIGN KEY (created_by)  REFERENCES users(id)
) ENGINE=InnoDB;

-- ── SEED ROOMS ──────────────────────────────────
INSERT IGNORE INTO rooms (room_number,type,floor,status,price,capacity,amenities,image_url) VALUES
('101','Deluxe',1,'available',150.00,2,'WiFi,TV,AC','https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=300&q=70'),
('102','Deluxe',1,'occupied',150.00,2,'WiFi,TV,AC','https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=300&q=70'),
('201','Superior',2,'cleaning',220.00,2,'WiFi,TV,AC,Bathtub','https://images.unsplash.com/photo-1566665797739-1674de7a421a?w=300&q=70'),
('202','Superior',2,'available',220.00,2,'WiFi,TV,AC,Bathtub','https://images.unsplash.com/photo-1566665797739-1674de7a421a?w=300&q=70'),
('301','Junior Suite',3,'occupied',320.00,3,'WiFi,TV,AC,Lounge,Nespresso','https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=300&q=70'),
('302','Junior Suite',3,'available',320.00,3,'WiFi,TV,AC,Lounge,Nespresso','https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=300&q=70'),
('401','Grand Suite',4,'maintenance',480.00,4,'WiFi,TV,AC,Jacuzzi,Bar,Dining','https://images.unsplash.com/photo-1578683010236-d716f9a3f461?w=300&q=70'),
('501','Presidential',5,'available',780.00,4,'WiFi,TV,AC,Jacuzzi,Butler,Terrace,Bar','https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=300&q=70');

-- ── SEED ADMIN (password: Admin@1234) ───────────
-- Run this PHP snippet once to get your hash:
-- echo password_hash('Admin@1234', PASSWORD_BCRYPT);
-- Then replace the hash below.
INSERT IGNORE INTO users (first_name,last_name,email,password,role) VALUES
('Admin','User','admin@aureliagrand.com','REPLACE_WITH_BCRYPT_HASH','admin');
