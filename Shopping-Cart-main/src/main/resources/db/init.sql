-- ============================================================
--  Ecommerce DB - Full Setup Script
--  Run this once against your MySQL server before starting the app
-- ============================================================

-- 1. Create & select database
CREATE DATABASE IF NOT EXISTS ecommerce_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE ecommerce_db;

-- ============================================================
-- 2. Schema
-- ============================================================

-- Category
CREATE TABLE IF NOT EXISTS category (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(255)  NOT NULL,
    image_name  VARCHAR(255),
    is_active   TINYINT(1)    DEFAULT 1
) ENGINE=InnoDB;

-- Product
CREATE TABLE IF NOT EXISTS product (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(500),
    description     VARCHAR(5000),
    category        VARCHAR(255),
    price           DOUBLE,
    stock           INT           DEFAULT 0,
    image           VARCHAR(255),
    discount        INT           DEFAULT 0,
    discount_price  DOUBLE,
    is_active       TINYINT(1)    DEFAULT 1
) ENGINE=InnoDB;

-- UserDtls
CREATE TABLE IF NOT EXISTS user_dtls (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(255),
    mobile_number       VARCHAR(20),
    email               VARCHAR(255) UNIQUE,
    address             VARCHAR(500),
    city                VARCHAR(255),
    state               VARCHAR(255),
    pincode             VARCHAR(20),
    password            VARCHAR(500),
    profile_image       VARCHAR(255),
    role                VARCHAR(50),
    is_enable           TINYINT(1)   DEFAULT 1,
    account_non_locked  TINYINT(1)   DEFAULT 1,
    failed_attempt      INT          DEFAULT 0,
    lock_time           DATETIME,
    reset_token         VARCHAR(255)
) ENGINE=InnoDB;

-- OrderAddress
CREATE TABLE IF NOT EXISTS order_address (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    first_name  VARCHAR(255),
    last_name   VARCHAR(255),
    email       VARCHAR(255),
    mobile_no   VARCHAR(20),
    address     VARCHAR(500),
    city        VARCHAR(255),
    state       VARCHAR(255),
    pincode     VARCHAR(20)
) ENGINE=InnoDB;

-- Cart
CREATE TABLE IF NOT EXISTS cart (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT,
    product_id  INT,
    quantity    INT DEFAULT 1,
    CONSTRAINT fk_cart_user    FOREIGN KEY (user_id)    REFERENCES user_dtls(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_product FOREIGN KEY (product_id) REFERENCES product(id)   ON DELETE CASCADE
) ENGINE=InnoDB;

-- ProductOrder
CREATE TABLE IF NOT EXISTS product_order (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    order_id         VARCHAR(255),
    order_date       DATE,
    product_id       INT,
    price            DOUBLE,
    quantity         INT,
    user_id          INT,
    status           VARCHAR(100),
    payment_type     VARCHAR(100),
    order_address_id INT,
    CONSTRAINT fk_order_product  FOREIGN KEY (product_id)       REFERENCES product(id),
    CONSTRAINT fk_order_user     FOREIGN KEY (user_id)          REFERENCES user_dtls(id),
    CONSTRAINT fk_order_address  FOREIGN KEY (order_address_id) REFERENCES order_address(id)
) ENGINE=InnoDB;

-- ============================================================
-- 3. Seed Data
-- ============================================================

-- ---- Categories ----
INSERT IGNORE INTO category (id, name, image_name, is_active) VALUES
(1, 'Mobile',      'mobile.png',      1),
(2, 'Laptop',      'laptop.jpg',      1),
(3, 'TV',          'tv.jpg',          1),
(4, 'Shoes',       'shoes.jpg',       1),
(5, 'Clothing',    'clothing.jpg',    1),
(6, 'Books',       'book.jpg',        1),
(7, 'Electronics', 'electronics.jpg', 1),
(8, 'Furniture',   'furniture.jpg',   1);

-- ---- Products ---- (images mapped to existing files in product_img/)
INSERT IGNORE INTO product (id, title, description, category, price, stock, image, discount, discount_price, is_active) VALUES
(1,  'iPhone 15 Pro',        'Latest Apple flagship with A17 Pro chip, titanium design, and advanced camera system.',     'Mobile',      129999, 50,  'iphone 14.jpg',        10, 116999, 1),
(2,  'Samsung Galaxy S24',   'Samsung flagship with Snapdragon 8 Gen 3, 200MP camera, and AI features.',                 'Mobile',      79999,  80,  'mobile.jpg',            5,  75999, 1),
(3,  'OnePlus 12',           'Flagship killer with Hasselblad camera, 100W SUPERVOOC charging.',                         'Mobile',      64999,  100, 'oneplus mobile.jpg',   15,  55249, 1),
(4,  'MacBook Air M3',       'MacBook Air with M3 chip, 18-hour battery life, and stunning Liquid Retina display.',      'Laptop',      134900, 30,  'laptop.jpg',            8, 124108, 1),
(5,  'Dell XPS 15',          'Premium Windows laptop with 13th Gen Intel Core i9, OLED display, NVIDIA RTX 4070.',      'Laptop',      189999, 20,  'hp laptop.jpg',         5, 180499, 1),
(6,  'Lenovo ThinkPad X1',   'Business ultrabook with Intel Core i7, 2K IPS display, military-grade durability.',       'Laptop',      109999, 40,  'laptop.jpg',           12,  96799, 1),
(7,  'Sony Bravia 55" 4K',   'OLED 4K TV with XR Processor, Dolby Vision, and Google TV interface.',                    'TV',          89990,  25,  'monitor.jpg',          10,  80991, 1),
(8,  'LG QNED 65"',          '65-inch QNED MiniLED TV with α7 AI Processor, 120Hz, and webOS 23.',                     'TV',          79990,  30,  'monitor.jpg',           7,  74391, 1),
(9,  'Nike Air Max 270',     'Iconic Nike sneakers with Max Air cushioning, available in multiple colorways.',           'Shoes',       12995,  200, 'lofer.jfif',           20,  10396, 1),
(10, 'Adidas Ultraboost 23', 'Premium running shoes with BOOST midsole and Primeknit+ upper for ultimate comfort.',     'Shoes',       17999,  150, 'white shoe.jfif',      15,  15299, 1),
(11, 'Levis 511 Slim Jeans', 'Classic slim-fit jeans in stretch denim, available in multiple washes.',                  'Clothing',    3999,   500, 'jeans blue.jfif',      10,   3599, 1),
(12, 'Allen Solly Formal Shirt','Premium cotton formal shirt, wrinkle-free and easy-care fabric.',                      'Clothing',    1999,   300, 'blue shirt.jfif',      15,   1699, 1),
(13, 'Atomic Habits',        'James Clear number 1 NYT bestseller on building good habits and breaking bad ones.',      'Books',       599,    1000, 'book_atomichabits.jpg',20,    479, 1),
(14, 'Rich Dad Poor Dad',    'Robert Kiyosaki classic personal finance book, a must-read for financial freedom.',       'Books',       499,    800,  'book_richdad.jpg',     10,    449, 1),
(15, 'boAt Airdopes 141',    'True wireless earbuds with 42H playback, Beast Mode low latency, and IPX4 rating.',       'Electronics', 1299,   500,  'earbuds_boat.jpg',     30,    909, 1),
(16, 'JBL Charge 5',         'Portable waterproof Bluetooth speaker with 20H playtime and powerbank feature.',          'Electronics', 13999,  100,  'speaker_jbl.jpg',       8,  12879, 1),
(17, 'IKEA MALM Bed Frame',  'Sturdy queen-size bed frame in white with smooth lines and Scandinavian design.',         'Furniture',   19999,   15,  'furniture_bed.jpg',    10,  17999, 1),
(18, 'Wakefit Orthopaedic Mattress','High-density foam mattress with memory foam layer for perfect back support.',     'Furniture',   12999,   40,  'furniture_mattress.jpg',12, 11439, 1);

-- ---- Admin User ----
-- Password: Admin@123 (BCrypt encoded below — change if needed)
INSERT IGNORE INTO user_dtls
    (id, name, mobile_number, email, address, city, state, pincode, password, profile_image, role, is_enable, account_non_locked, failed_attempt, lock_time, reset_token)
VALUES
(1, 'Admin',
    '9999999999',
    'admin@ecomstore.com',
    '123 Admin Street',
    'Mumbai',
    'Maharashtra',
    '400001',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBpwTpyB3JCe6',  -- Admin@123
    'default_user.png',
    'ROLE_ADMIN',
    1, 1, 0, NULL, NULL);
