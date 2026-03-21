# 💳 FinTech Payment Backend

A **production-grade, distributed fintech payment backend** built with Spring Boot Microservices — inspired by real-world systems like PayPal and Stripe.

![Java](https://img.shields.io/badge/Java-17-orange?style=flat&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen?style=flat&logo=springboot)
![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-7.5-black?style=flat&logo=apachekafka)
![Docker](https://img.shields.io/badge/Docker-Compose-blue?style=flat&logo=docker)
![H2](https://img.shields.io/badge/Database-H2-lightblue?style=flat)
![JWT](https://img.shields.io/badge/Auth-JWT-yellow?style=flat&logo=jsonwebtokens)
![Status](https://img.shields.io/badge/Status-Completed-success?style=flat)

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Architecture](#-architecture)
- [Services](#-services)
- [Tech Stack](#-tech-stack)
- [Getting Started](#-getting-started)
- [API Reference](#-api-reference)
- [Key Design Patterns](#-key-design-patterns)
- [Project Structure](#-project-structure)

---

## 🌐 Overview

This system implements a **microservices-based payment platform** with:

- Stateless JWT authentication
- Event-driven communication via Kafka
- Pessimistic locking for financial safety
- Idempotency protection across all services
- Centralized routing via API Gateway
- Docker Compose for single-command deployment

---

## 🏗 Architecture

```
                        ┌─────────────────┐
         HTTP :8080     │   API Gateway   │   JWT Validation
  Client ──────────────▶│  (Spring Cloud) │   Centralized Routing
                        └────────┬────────┘
                                 │
              ┌──────────────────┼────────────────────┐
              │                  │                    │
              ▼                  ▼                    ▼
      ┌──────────────┐  ┌──────────────┐   ┌──────────────────┐
      │ user-service │  │  transaction │   │  wallet-service  │
      │   :8081      │  │  service     │   │     :8090        │
      │  JWT + Auth  │  │   :8082      │   │ Pessimistic Lock │
      └──────────────┘  └──────┬───────┘   └──────────────────┘
                               │
                               │ Kafka (txn-initiated)
                               ▼
                    ┌──────────────────────┐
                    │     Apache Kafka     │
                    │    + Zookeeper       │
                    └──────┬───────────────┘
                           │
              ┌────────────┴────────────┐
              ▼                         ▼
   ┌─────────────────────┐   ┌──────────────────┐
   │ notification-service│   │  reward-service  │
   │      :8084          │   │     :8085        │
   │  Kafka Consumer     │   │  Kafka Consumer  │
   └─────────────────────┘   └──────────────────┘
```

---

## 📦 Services

| Service | Port | Responsibility |
|---------|------|----------------|
| `api-gateway` | 8080 | Centralized routing, JWT enforcement |
| `user-service` | 8081 | Signup, Login, JWT issuance |
| `transaction-service` | 8082 | Create transactions, publish Kafka events |
| `notification-service` | 8084 | Consume events, send notifications |
| `reward-service` | 8085 | Consume events, calculate reward points |
| `wallet-service` | 8090 | Balance management, holds, credits, debits |

---

## 🛠 Tech Stack

| Category | Technology |
|----------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.x |
| API Gateway | Spring Cloud Gateway (WebFlux) |
| Messaging | Apache Kafka + Zookeeper |
| Auth | JWT (Stateless) |
| Security | Spring Security |
| Database | H2 (In-Memory) |
| ORM | Spring Data JPA / Hibernate |
| Containerization | Docker + Docker Compose |
| Build Tool | Maven |

---

## 🚀 Getting Started

### Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running
- Java 17+ (only needed for local development without Docker)

### Run with Docker (Recommended)

```bash
# 1. Clone the repository
git clone https://github.com/yourusername/FinTech-Payment-Backend.git
cd FinTech-Payment-Backend

# 2. Start all services with a single command
docker-compose up -d --build

# 3. Verify all services are running
docker-compose ps
```

All 8 containers (Zookeeper, Kafka + 6 microservices) will start automatically.

### Useful Commands

```bash
# Stop all services
docker-compose down

# View logs of all services
docker-compose logs -f

# View logs of a specific service
docker-compose logs -f user-service

# Restart a specific service
docker-compose restart transaction-service
```

---

## 📡 API Reference

All requests go through the **API Gateway at `http://localhost:8080`**

### Auth (User Service)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/auth/signup` | Register a new user | ❌ |
| POST | `/auth/login` | Login and receive JWT token | ❌ |

### Transactions

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/transactions` | Create a transaction | ✅ |
| GET | `/api/transactions` | Get all transactions | ✅ |

### Rewards

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/rewards` | Get reward points | ✅ |

### Notifications

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/notifications` | Get notifications | ✅ |

### Authentication Header

```
Authorization: Bearer <your-jwt-token>
```

---

## 🧠 Key Design Patterns

### 🔐 JWT Authentication (Stateless)
- Token issued on login containing `sub` (email) and `role`
- Validated at Gateway level — no session, no DB lookup in filter
- All downstream services trust forwarded requests

### 📨 Event-Driven Architecture (Kafka)
```
Transaction Created → txn-initiated topic → Notification Service
                                          → Reward Service
```
- Producer: `transaction-service`
- Consumers: `notification-service` (group: `notification-group`), `reward-service` (group: `reward-group`)

### 🔒 Pessimistic Locking (Wallet)
- `@Lock(LockModeType.PESSIMISTIC_WRITE)` on all balance mutations
- Prevents race conditions and double spending
- Atomic debit/credit operations

### 🔁 Idempotency (Reward & Notification)
- Kafka guarantees at-least-once delivery
- Application-level check: `existsByTransactionId()`
- DB-level: `@Column(unique = true)` on `transaction_id`
- Implements the **Idempotent Consumer Pattern**

### 💰 Hold–Capture–Release (Wallet)
```
Hold Funds → (success) → Capture Hold → Finalized Debit
           → (failure) → Release Hold → Funds Returned
```
- Unique `holdReference` per hold
- Auto-expiry via `@Scheduled` cleanup job

---

## 📁 Project Structure

```
FinTech-Payment-Backend/
├── api-gateway/
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── user-service/
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── transaction-service/
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── notification-service/
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── reward-service/
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── wallet-service/
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── docker-compose.yml
└── README.md
```

---

## 🔐 Financial Safety Highlights

- ✅ Atomic wallet balance updates
- ✅ Pessimistic locking — no double spending
- ✅ Hold → Capture → Release workflow
- ✅ Idempotent Kafka consumers — safe retries
- ✅ Scheduled recovery for expired holds
- ✅ Custom exceptions with proper HTTP status codes

---

## 📌 What I Learned

- Designing distributed microservice communication with Kafka
- Implementing stateless JWT auth across a gateway + multiple services
- Applying pessimistic locking for financial concurrency control
- Idempotency patterns for at-least-once Kafka delivery
- Containerizing a multi-service Spring Boot system with Docker Compose

---

## 👨‍💻 Author

**Brijesh** — [GitHub](https://github.com/BrijeshPatra) · [LinkedIn](https://www.linkedin.com/in/brijeshpatra/)

---

> ⭐ If you found this project useful, consider giving it a star!
