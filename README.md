# 🛒 Ecom Store — Full-Stack E-Commerce Application

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.3-brightgreen?style=flat-square&logo=springboot)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?style=flat-square&logo=mysql)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-3.1-darkgreen?style=flat-square&logo=thymeleaf)
![Gemini AI](https://img.shields.io/badge/Gemini%202.5%20Flash-AI%20Chatbot-purple?style=flat-square&logo=google)
![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)

A production-ready, full-stack e-commerce platform built with **Spring Boot 3**, **MySQL**, and **Thymeleaf**. Features a complete shopping experience with user authentication, admin panel, cart management, order processing, and an **AI-powered shopping assistant** using Google Gemini 2.5 Flash with Retrieval-Augmented Generation (RAG).

---

## 📋 Table of Contents

- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [AI Shopping Assistant](#-ai-shopping-assistant)
- [Getting Started](#-getting-started)
- [Configuration](#-configuration)
- [Admin Panel](#-admin-panel)
- [Screenshots](#-screenshots)

---

## ✨ Features

### 🛍️ Customer Features
- Browse products by category with search and filtering
- Add to cart, update quantities, remove items
- Secure checkout with Cash on Delivery and Online Payment
- Order history and order tracking
- User registration and login with Spring Security
- Forgot password / reset password via email link

### 🔧 Admin Features
- Full product management — add, edit, delete with image upload
- Category management with custom images
- Order management — view all orders and update status
- User account management — enable / disable accounts
- Dashboard with pagination and search

### 🤖 AI Shopping Assistant
- Floating chatbot widget on every page
- Powered by **Google Gemini 2.5 Flash** + **RAG** pattern
- Understands natural language queries: _"cheapest laptops in stock"_, _"shoes under ₹5000"_
- Returns real-time product data from the database as clickable cards
- Smart fallback when AI is unavailable

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **Backend** | Java 17, Spring Boot 3.2.3 |
| **Web MVC** | Spring MVC, Thymeleaf 3.1 |
| **Security** | Spring Security 6 |
| **Database** | MySQL 8.0 via Spring Data JPA (Hibernate) |
| **Connection Pool** | HikariCP |
| **AI / Chatbot** | Google Gemini 2.5 Flash API (RAG pattern) |
| **Build Tool** | Apache Maven |
| **Frontend** | Bootstrap 5.3.3, Vanilla CSS, jQuery |
| **Mail** | Spring Mail (SMTP — for password reset) |
| **Dev Tools** | Spring DevTools (hot reload) |
| **Lombok** | Boilerplate reduction |

---

## 📁 Project Structure

```
src/
├── main/
│   ├── java/com/ecom/
│   │   ├── controller/
│   │   │   ├── AdminController.java      # Admin CRUD for products, categories, orders
│   │   │   ├── HomeController.java       # Public pages, login, registration, password reset
│   │   │   ├── UserController.java       # Cart, checkout, order history
│   │   │   └── ChatController.java       # POST /api/chat — AI chatbot endpoint
│   │   ├── service/
│   │   │   ├── ChatService.java          # RAG logic — intent extraction + DB query + prompt
│   │   │   ├── GeminiService.java        # Google Gemini REST API client
│   │   │   ├── impl/
│   │   │   │   ├── OrderServiceImpl.java
│   │   │   │   ├── ProductServiceImpl.java
│   │   │   │   └── UserServiceImpl.java
│   │   ├── repository/
│   │   │   ├── ProductRepository.java    # JPA queries (price, category, stock filters)
│   │   │   ├── CategoryRepository.java
│   │   │   └── ProductOrderRepository.java
│   │   ├── model/
│   │   │   ├── Product.java
│   │   │   ├── Category.java
│   │   │   ├── UserDtls.java
│   │   │   ├── Cart.java
│   │   │   └── ProductOrder.java
│   │   ├── config/
│   │   │   └── SecurityConfig.java       # Spring Security rules
│   │   └── util/
│   │       ├── CommonUtil.java           # Mail sender, URL generator
│   │       └── OrderStatus.java          # Order status enum
│   └── resources/
│       ├── application.properties        # All configuration
│       ├── db/
│       │   └── init.sql                  # Database schema + seed data
│       ├── templates/                    # Thymeleaf HTML templates
│       └── static/
│           ├── css/style.css
│           ├── js/script.js
│           └── img/                      # Product and category images
```

---

## 🤖 AI Shopping Assistant

The chatbot uses **RAG (Retrieval-Augmented Generation)** — an industry-standard AI pattern that prevents hallucination by grounding the AI in your real database.

### How it works

```
User message
    │
    ▼
ChatService — Extract intent (category, price, stock)
    │
    ▼
ProductRepository — Query MySQL for matching products
    │
    ▼
Prompt Builder — Inject real product data as AI context
    │
    ▼
GeminiService — POST to Gemini 2.5 Flash API
    │
    ▼
Response — AI natural text + DB product cards (clickable)
```

### Example queries the bot handles
| Query | What happens |
|---|---|
| `"mobiles under ₹20,000"` | Filters by category=Mobile, price≤20000 |
| `"cheapest laptops in stock"` | Filters stock>0, sorts by price ascending |
| `"do you have running shoes?"` | Keyword matches → Shoes category search |
| `"show me electronics"` | Returns all Electronics category products |
| `"hello"` | Returns a welcome message with usage examples |

---

## 🚀 Getting Started

### Prerequisites
- Java 17 or higher
- MySQL 8.0
- Maven (or use the included `mvnw` wrapper)

### 1. Clone the repository
```bash
git clone https://github.com/YOUR_USERNAME/ecom-store.git
cd ecom-store
```

### 2. Set up the database
```sql
CREATE DATABASE ecommerce_db;
```
Then run the seed script:
```bash
mysql -u root -p ecommerce_db < src/main/resources/db/init.sql
```
This creates all tables and inserts:
- 8 categories (Mobile, Laptop, TV, Shoes, Clothing, Books, Electronics, Furniture)
- 18 sample products with images and discount prices
- 1 admin account

### 3. Configure application.properties
Update your database credentials in `src/main/resources/application.properties`:
```properties
spring.datasource.username=your_mysql_username
spring.datasource.password=your_mysql_password
```

### 4. Run the application
```bash
./mvnw spring-boot:run
```
App starts at **http://localhost:8080**

---

## ⚙️ Configuration

All configuration lives in [`application.properties`](src/main/resources/application.properties).

### Environment Variables (recommended for production)

| Variable | Description | Default (local) |
|---|---|---|
| `DB_URL` | MySQL JDBC URL | `jdbc:mysql://localhost:3306/ecommerce_db` |
| `DB_USERNAME` | MySQL username | `root` |
| `DB_PASSWORD` | MySQL password | *(local password)* |
| `GEMINI_API_KEY` | Google Gemini API key | *(included for dev)* |
| `PORT` | Server port (auto-set by Railway/Render) | `8080` |

### Getting a Gemini API Key
1. Go to [Google AI Studio](https://aistudio.google.com)
2. Click **Get API Key** → **Create API Key**
3. Set it as env var: `GEMINI_API_KEY=your_key_here`

---

## 🔐 Admin Panel

| Field | Value |
|---|---|
| **URL** | `http://localhost:8080/signin` |
| **Email** | `admin@ecomstore.com` |
| **Password** | `Admin@123` |

> Any email containing `admin@` automatically receives `ROLE_ADMIN`.

### Admin Capabilities
- `/admin/products` — View, search, paginate all products
- `/admin/loadAddProduct` — Add new product with image
- `/admin/editProduct/{id}` — Edit product details and pricing
- `/admin/deleteProduct/{id}` — Remove a product
- `/admin/categories` — Manage categories
- `/admin/orders` — View and update all order statuses
- `/admin/users` — Enable / disable customer accounts

---

## 📸 Screenshots

> Run the app locally and visit `http://localhost:8080` to see the full UI.

| Page | URL |
|---|---|
| Homepage | `/` |
<img width="1863" height="834" alt="image" src="https://github.com/user-attachments/assets/98009ec4-b860-4433-a124-d2b5960c0ba3" />

| Product Search | `/products?category=Mobile` |
<img width="1839" height="897" alt="image" src="https://github.com/user-attachments/assets/ccc624b1-64f7-48a6-b0b0-ef61cb0b59e9" />

| Cart | `/user/cart` |
<img width="1891" height="855" alt="image" src="https://github.com/user-attachments/assets/67dc2a05-852a-463c-9d16-af904b3bff19" />

| Checkout | `/user/checkout` |
<img width="1875" height="850" alt="image" src="https://github.com/user-attachments/assets/0ee64c9b-1925-41a9-8249-28dbaad11d38" />

| Order History | `/user/orders` |
<img width="1887" height="851" alt="image" src="https://github.com/user-attachments/assets/17ee6321-c303-40a2-a7f2-5df3a5b6940a" />

| Admin Dashboard | `/admin/products` |
<img width="1893" height="840" alt="image" src="https://github.com/user-attachments/assets/44e83092-1369-47ac-a7b0-982840983eeb" />

| AI Chatbot | Available on every page (bottom-right widget) |
<img width="527" height="820" alt="image" src="https://github.com/user-attachments/assets/ba423c6e-ee6c-4482-9bfe-2cf8b4da731d" />

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).

---
