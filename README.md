# User Service + Spring Security + JWT

This document captures **all notes, decisions, and implementations done so far**, written in a way that also works as a **README**. Tomorrow, we will extend this with **DTOs and mapping**.

---

## 1. Project Overview

This is a **Spring Boot User Service** designed in an **industry-standard, RESTful way**, with:

* Clean controller–service–repository layering
* Proper HTTP method usage
* Spring Security integration
* JWT-based authentication (stateless)

The goal is to build something **production-realistic** (PayPal/Stripe-style backend).

---

## 2. REST API Design (Industry Standard)

Base path:

```
/users
```

| Operation      | HTTP Method | Endpoint    |
| -------------- | ----------- | ----------- |
| Create user    | POST        | /users      |
| Get all users  | GET         | /users      |
| Get user by ID | GET         | /users/{id} |

### Key REST Rules Followed

* GET → read-only (no request body)
* POST → create resources
* Path variables used for IDs
* Plural resource naming (`/users`)

---

## 3. Controller Layer Decisions

### ✅ Correct Patterns Used

* Controller depends on **Service Interface**, not implementation
* Uses `ResponseEntity` for proper HTTP status handling
* Returns `404 Not Found` when user does not exist

### Example (Get User by ID)

```java
@GetMapping("/{id}")
public ResponseEntity<User> getUserById(@PathVariable Long id) {
    return userService.getUserById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
}
```

---

## 4. Service Layer

### Key Points

* Business logic lives here
* Repository access abstracted behind service
* Returns Optional for safe null handling

### Constructor Injection

```java
public UserServiceImpl(UserRepository userRepository) {
    this.userRepository = userRepository;
}
```

Why:

* Loose coupling
* Testable
* Recommended by Spring

---

## 5. Spring Security – Initial Behavior

### What Happened

After adding:

```xml
spring-boot-starter-security
```

All endpoints returned:

```
401 Unauthorized
```

### Why (Important)

* Spring Security secures **all endpoints by default**
* Requires authentication unless configured otherwise

This is **expected behavior**, not a bug.

---

## 6. CSRF – Why It Was Disabled

### What CSRF Protects Against

* CSRF attacks exploit **browser cookies + sessions**

### Why Disabled Here

This project is a:

* Stateless REST API
* Uses JWT (Bearer tokens)
* No cookies involved

➡ CSRF protection is **not needed**

```java
http.csrf(csrf -> csrf.disable());
```

Rule to remember:

> CSRF is needed only for cookie-based authentication

---

## 7. JWT Authentication Flow

### Authentication Model

* Stateless
* Token-based (JWT)
* Token sent via `Authorization` header

```
Authorization: Bearer <jwt-token>
```

---

## 8. JwtRequestFilter – Purpose

`JwtRequestFilter` extends `OncePerRequestFilter` and:

1. Runs once per request
2. Extracts JWT from header
3. Validates token
4. Extracts username and role
5. Sets authentication in `SecurityContextHolder`
6. Allows request to reach controller

---

## 9. Common Mistakes Fixed

### ❌ Missing `filterChain.doFilter()`

* Caused requests to never reach controller

### ❌ Duplicate endpoint mappings

* Same `@GetMapping("/{id}")` twice → ambiguous mapping error

### ❌ GET with RequestBody

* Violates REST + HTTP spec

### ❌ Casting List<User> to User

* Fixed by returning `ResponseEntity<List<User>>`

---

## 10. Logging Strategy (JWT Filter)

### Logger Used

* SLF4J (`LoggerFactory`)

### Logged Events

* JWT extraction attempt
* Username extraction success/failure
* Token validation success/failure
* Role extraction
* Authentication setup

Logging levels:

* `debug` → normal auth flow
* `warn` → invalid token
* `error` → unexpected failures

---

## 11. Security Context Understanding

Once authentication is set:

```java
SecurityContextHolder.getContext().setAuthentication(authToken);
```

Then:

* Controllers trust the user is authenticated
* Role-based access can be applied later

---

## 12. What We Have NOW

✅ RESTful controllers
✅ Proper HTTP semantics
✅ Service abstraction
✅ Spring Security configured
✅ JWT filter implemented
✅ Logging in place

This is a **solid backend foundation**.

---

## 13. Next Step (Tomorrow)

### DTO Implementation

We will:

* Introduce UserRequestDTO / UserResponseDTO
* Remove Entity exposure from API
* Add validation annotations
* Map DTO ↔ Entity

This will make the API:

* Safer
* Cleaner
* Production-ready

---

## ✅ JWT + DTO IMPLEMENTATION (COMPLETED)

### 🔐 Authentication Flow

* **Signup**

  * Uses `SignupRequest` DTO
  * Checks existing user by email
  * Password encoding handled in service layer
  * Default role assigned: `ROLE_USER`

* **Login**

  * Uses `LoginRequest` DTO
  * Validates user existence
  * Validates password using `PasswordEncoder`
  * Generates JWT token using `JwtUtil`
  * Returns token via `LoginResponse` DTO

### 🧾 DTOs Used

* `SignupRequest`
* `LoginRequest`
* `LoginResponse`

> DTOs are **NOT injected** as Spring beans. They are created per request to avoid lifecycle and thread-safety issues.

### 🔑 JWT Details

* Stateless authentication
* Token contains:

  * `sub` (email)
  * `role` (custom claim)
* Token validated in `JwtRequestFilter`
* No DB calls in filter (pure JWT-based auth)

### 🛡️ Security Filter

* Custom `JwtRequestFilter` extends `OncePerRequestFilter`
* Extracts token from `Authorization: Bearer <token>`
* Validates token using `JwtUtil`
* Sets `SecurityContext` with role-based authority

### 🚫 Common Pitfalls Avoided

* No DTO injection into services
* No business logic in controllers
* No session-based authentication
* No DB access in security filter

## ✅ Transaction Service + Kafka Producer: Event send from KafkaProducer (COMPLETED)

Transaction Service → Kafka (txn-initiated)
                          ↓
                 Notification Service
Atomic Operations: Uses @Transactional to ensure data consistency between the DB and Kafka.

Event Generation: Publishes a TransactionEvent to the txn-initiated topic upon successful record creation.


## ✅ Notification Service + Kafka Listener: Event consumed by KafkaListener (COMPLETED)

* Consumer Group: Scalable via notification-group.

* Deduplication: The service logic is designed to check for existing records to prevent duplicate notifications (At-least-once delivery handling).

Key Patterns Used

* Constructor Injection: For better testability and loose coupling.

* Global Logging: Structured SLF4J logging for JWT filters and Kafka listeners.

### ✅ Reward Service + Kafka Listener: Event Consumed by KafkaListener (COMPLETED)

The Reward Service is fully event-driven and consumes transaction events published by the Transaction Service via Kafka.

#### 🔄 Event Consumption

Topic: txn-initiated

Consumer Group: reward-group

Listener: @KafkaListener

Auto Offset Reset: earliest

JSON Deserialization using Spring Kafka JsonDeserializer

The service listens for transaction events and processes only SUCCESS transactions.

#### 🎯 Reward Processing Logic

When a transaction event is received:

Validate transaction status (SUCCESS only).

Perform idempotency check using transactionId.

Calculate reward points (percentage-based logic).

Persist reward record in database.

Log structured success/failure events.

#### 🛡 Idempotency Handling (At-Least-Once Safe)

Kafka guarantees at-least-once delivery, meaning events may be redelivered.

To prevent duplicate rewards:

Application-level check:

existsByTransactionId(transactionId)

Database-level protection:

@Column(unique = true) on transaction_id

Unique constraint at table level

This implements the Idempotent Consumer Pattern, ensuring safe retry handling.

Logging via LoggerFactory(Slf4j)

Database indexing for performance (user_id, transaction_id)

🔐 Separation of Concerns
Layer	Responsibility
Kafka Listener	Event consumption only
Service Layer	Business logic (reward calculation + idempotency)
Repository	Persistence operations
Entity	Database mapping
Controller Layer: Exposed simple endpoints

No business logic inside Kafka listener.

#### 📊 Reward Calculation Strategy

Current implementation:

Percentage-based reward (e.g., 2% of transaction amount)

Designed to be easily extendable to:

Tier-based rewards

Campaign-based bonuses

Dynamic rule engine

Configurable reward slabs

## ✅ API Gateway (COMPLETED)

The API Gateway is fully implemented and serves as the single entry point for all backend services.

### 🚪 Gateway Responsibilities

- Centralized routing to all microservices:
  - `/auth/**` → User Service
  - `/api/transactions/**` → Transaction Service
  - `/api/rewards/**` → Reward Service
  - `/api/notifications/**` → Notification Service

- JWT validation at gateway level
- Stateless request forwarding
- Reactive architecture using Spring Cloud Gateway
- Centralized cross-cutting concerns (authentication, logging, filters)

---

### 🔐 Security at Gateway

- Custom JWT validation filter implemented
- Extracts token from: Authorization: Bearer <token>
- Validates token using shared `JwtUtil`
- Rejects unauthorized requests before reaching services
- No session usage (fully stateless)

---

### ⚙️ Architecture Decisions

- Reactive (WebFlux-based)
- No business logic in gateway
- No database interaction
- Pure routing + security enforcement
- Microservices remain independently deployable

---

### 🧠 Design Principles Followed

| Layer        | Responsibility              |
|-------------|-----------------------------|
| Gateway     | Routing + Security Enforcement |
| Microservices | Business Logic |
| Kafka       | Event Communication |
| Database    | Persistence |
| JWT         | Stateless Authentication |

---

### 📊 Current Status

- User Service: ✅ Complete  
- JWT + DTO: ✅ Complete  
- Transaction Service: Complete  
- Notification Service: Complete  
- Reward service design  
- Idempotency protection: ✅ Complete  
- API Gateway: ✅ Complete  

### 📌 Current Status

* User Service: ✅ Complete
* JWT + DTO: ✅ Complete
* Transaction Service: Complete
* Notification Service: Complete
* Reward service design
* Idempotency protection: ✅ Complete

# ✅ Wallet Service (COMPLETED)

The Wallet Service is responsible for maintaining **user wallet balances**, handling **debits, credits, holds, captures, and releases** with strong consistency guarantees.

It is designed using **banking-grade concurrency control** and **idempotent financial operations**.

---

## 🏗 Core Responsibilities

- Wallet creation per user
- Credit / Debit operations
- Temporary fund holds
- Capture of held funds
- Automatic hold expiry via scheduler
- Concurrency-safe balance updates
- Exception handling for insufficient funds and invalid operations

---

## 🔐 Concurrency Control – Pessimistic Locking

To prevent race conditions during concurrent balance updates:

- Implemented **Pessimistic Locking**
- Used `@Lock(LockModeType.PESSIMISTIC_WRITE)`
- Ensures only one transaction modifies wallet balance at a time

### Guarantees

- No double spending
- No negative balance due to race conditions
- Strong transactional consistency

This mirrors real-world fintech wallet systems.

---

## 💰 Wallet Operations Implemented

### 1️⃣ Create Wallet

- Initializes wallet for user
- Sets initial balance to zero
- Enforces unique wallet per user

---

### 2️⃣ Credit Wallet

- Adds amount to available balance
- Fully transactional
- Uses pessimistic locking

---

### 3️⃣ Debit Wallet

- Validates sufficient balance
- Deducts from available balance
- Throws `InsufficientFundException` if invalid

---

### 4️⃣ Hold Funds

- Deducts amount from available balance
- Moves amount to "held" state
- Generates unique `holdReference`
- Sets expiration timestamp
- Fully idempotent and transactional

#### Used for:
- Payment authorization
- Reserve-before-capture workflows

---

### 5️⃣ Capture Hold

- Converts held amount into finalized debit
- Updates wallet state accordingly
- Ensures hold exists and is active

---

### 6️⃣ Release Hold

- Returns held amount back to available balance
- Used when payment fails or expires

---

## ⏳ Hold Expiry Scheduler

Implemented scheduled cleanup mechanism:

- `@Scheduled` job scans expired holds
- Automatically releases expired holds
- Reuses existing release logic
- Logs success/failure without blocking execution

### Design Considerations

- Does not crash on single failure
- Continues processing remaining holds
- Safe for production workloads

---

## 🛡 Exception Handling

Custom exceptions implemented:

- `InsufficientFundException`
- `NotFoundException`

### Global Handling Strategy

- Clear error messages
- Proper HTTP status codes
- No internal exception leakage

---

## 🔁 Idempotency Strategy

To prevent duplicate financial operations:

- Unique `holdReference`
- Database constraints
- Validation before mutation
- Transactional boundaries

### Ensures

- Safe retries
- At-least-once safe processing
- Financial correctness

---

## 🧠 Architecture Design

| Layer       | Responsibility                  |
|------------|----------------------------------|
| Controller | Request handling only            |
| Service    | Business logic & locking         |
| Repository | DB access + locking              |
| Entity     | DB mapping                       |
| Scheduler  | Expiry cleanup                   |

- No business logic inside controllers.
- All balance mutations are transactional.

---

## 📊 Data Integrity Guarantees

- ✔ Atomic balance updates  
- ✔ Concurrency-safe operations  
- ✔ Hold-expiry auto recovery  
- ✔ Clear separation of available vs held balance  
- ✔ Strong financial correctness  

---

## 📌 Current Status (Updated)

- User Service: ✅ Complete  
- JWT + DTO: ✅ Complete  
- Transaction Service: ✅ Complete  
- Notification Service: ✅ Complete  
- Reward Service: ✅ Complete  
- Idempotency protection: ✅ Complete  
- API Gateway: ✅ Complete  
- Wallet Service: ✅ Complete  
- Pessimistic Locking: ✅ Implemented  
- Hold & Capture Flow: ✅ Implemented  
- Scheduler for Expiry: ✅ Implemented  
- Exception Handling: ✅ Implemented

---

# 🏁 Project Conclusion

This project represents a **distributed fintech-style payment backend system** built using a microservices architecture.

The system includes:

- ✅ User Service (Authentication & JWT)
- ✅ Wallet Service (Balance, Hold, Capture, Concurrency Control)
- ✅ Transaction Service
- ✅ Notification Service
- ✅ Reward Service
- ✅ API Gateway (Routing & Centralized Entry Point)
- ✅ Idempotency Protection
- ✅ Scheduled Hold Expiry Handling
- ✅ Exception Handling & Validation
- ✅ Pessimistic Locking for Financial Safety

---

## 🏗 Architecture Summary

- Built using **Spring Boot Microservices**
- API Gateway using **Spring Cloud Gateway**
- Database integration with **JPA/Hibernate**
- Secure authentication using **JWT**
- Concurrency-safe financial operations
- Modular and scalable service design
- Clean layered architecture (Controller → Service → Repository → Entity)

---

## 🔐 Financial Safety Highlights

- Atomic wallet balance updates  
- Concurrency protection using pessimistic locking  
- Hold–Capture–Release payment workflow  
- Idempotent operations to prevent duplicate processing  
- Scheduled recovery for expired holds  

This ensures the system behaves similarly to real-world digital wallet and payment processing platforms.

---

## 🚀 Learning & Engineering Outcomes

Through this project:

- Implemented real-world fintech patterns
- Designed distributed system communication
- Handled transactional consistency challenges
- Applied concurrency control in financial operations
- Built scalable microservice architecture

---

## 📌 Final Note

This backend system is structured to serve as a **strong foundation for a production-grade fintech/payment platform like paypal**.  

It demonstrates practical implementation of:

- Microservices architecture  
- Secure authentication  
- Distributed routing  
- Financial transaction handling  
- Concurrency management  

The system is modular, extensible, and ready for further enhancements such as service discovery, centralized logging, monitoring, and deployment automation.

---

**Project Status: ✅ Completed**


