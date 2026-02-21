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

## Final Note

Everything done so far follows **industry best practices** and mirrors how real fintech backends are built.

Tomorrow we level it up with **DTOs and clean API contracts** 🚀

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


### 📌 Current Status

* User Service: ✅ Complete
* JWT + DTO: ✅ Complete
* Transaction Service: Complete
* Notification Service: Complete
* Reward service design
* Idempotency protection: ✅ Complete

---

Next planned:
* Wallet balance consistency
* Idempotent transactions
* Failure & rollback strategies
