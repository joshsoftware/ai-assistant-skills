# Mock Bank Service — Implementation Summary

## Branch
`feature/mock-bank-service`

## What Was Built
A production-ready Spring Boot 3.4.5 (Java 21) service simulating core banking operations,
generated using the `boilerplate` skill following Josh Software standards.

---

## Project Setup Choices

| Question | Answer |
|----------|--------|
| Build tool | Gradle (Groovy DSL) |
| Config format | application.properties |
| JWT authentication | No |
| Pagination on list APIs | Yes |
| Author | Apurva Rawal |

---

## Folder Location
```
java-claude-skills/
└── mock-bank-service/
    ├── build.gradle
    ├── settings.gradle
    └── src/
        ├── main/
        │   ├── java/com/joshsoftware/mockbank/
        │   │   ├── MockBankApplication.java
        │   │   ├── constants/
        │   │   ├── controller/
        │   │   ├── dto/
        │   │   │   ├── request/
        │   │   │   └── response/
        │   │   ├── entity/
        │   │   ├── enums/
        │   │   ├── exception/
        │   │   │   └── handler/
        │   │   ├── mapper/
        │   │   ├── repository/
        │   │   └── service/
        │   │       └── impl/
        │   └── resources/
        │       └── application.properties
        └── test/
            └── java/com/joshsoftware/mockbank/
                └── MockBankApplicationTests.java
```

---

## Domains Implemented

### 1. Customer
**Entity fields:** id (UUID), name, email, phone, address, kycStatus, createdAt, updatedAt, createdBy, updatedBy

**APIs:**
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/customers` | Create a new customer |
| GET | `/api/v1/customers` | List all customers (paginated) |
| PUT | `/api/v1/customers/{id}` | Update customer details |

**Files generated:**
- `enums/KycStatus.java` — PENDING, VERIFIED, REJECTED, UNDER_REVIEW
- `entity/Customer.java`
- `dto/request/CreateCustomerRequestDTO.java`
- `dto/request/UpdateCustomerRequestDTO.java`
- `dto/response/CustomerResponseDTO.java`
- `constants/CustomerConstants.java`
- `repository/CustomerRepository.java`
- `mapper/CustomerMapper.java`
- `service/CustomerService.java`
- `service/impl/CustomerServiceImpl.java`
- `controller/CustomerController.java`

---

### 2. Account
**Entity fields:** id (UUID), accountNumber, accountType, balance, currency, status, customer (ManyToOne), createdAt, updatedAt, createdBy, updatedBy

**APIs:**
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/accounts` | Create account linked to a customer |
| GET | `/api/v1/accounts/{id}` | Fetch account by ID |
| GET | `/api/v1/accounts/customer/{customerId}` | List all accounts for a customer |
| PATCH | `/api/v1/accounts/{id}/status` | Update account status |

**Files generated:**
- `enums/AccountType.java` — SAVINGS, CURRENT, FIXED_DEPOSIT
- `enums/AccountStatus.java` — ACTIVE, INACTIVE, FROZEN
- `entity/Account.java`
- `dto/request/CreateAccountRequestDTO.java`
- `dto/request/UpdateAccountStatusRequestDTO.java`
- `dto/response/AccountResponseDTO.java`
- `constants/AccountConstants.java`
- `repository/AccountRepository.java`
- `mapper/AccountMapper.java`
- `service/AccountService.java`
- `service/impl/AccountServiceImpl.java`
- `controller/AccountController.java`

---

### 3. Transaction
**Entity fields:** id (UUID), transactionId, type, amount, description, status, fromAccount (ManyToOne), toAccount (ManyToOne), timestamp, createdAt, updatedAt, createdBy, updatedBy

**APIs:**
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/transactions/{id}` | Fetch a single transaction |
| GET | `/api/v1/transactions/account/{accountId}` | List all transactions for an account (paginated) |

**Files generated:**
- `enums/TransactionType.java` — CREDIT, DEBIT, TRANSFER
- `enums/TransactionStatus.java` — PENDING, SUCCESS, FAILED
- `entity/Transaction.java`
- `dto/response/TransactionResponseDTO.java`
- `constants/TransactionConstants.java`
- `repository/TransactionRepository.java`
- `mapper/TransactionMapper.java`
- `service/TransactionService.java`
- `service/impl/TransactionServiceImpl.java`
- `controller/TransactionController.java`

---

### 4. Payment
**Entity fields:** id (UUID), paymentReference, paymentMode, amount, status, transaction (OneToOne), createdAt, updatedAt, createdBy, updatedBy

**APIs:**
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/payments` | Initiate a payment (debit + credit + create transaction) |
| GET | `/api/v1/payments/{id}` | Fetch payment status |
| GET | `/api/v1/payments` | List all payments (paginated) |

**Business logic in `initiate()`:**
- Validates both accounts exist and are ACTIVE
- Checks sufficient balance in source account
- Debits `fromAccount`, credits `toAccount`
- Persists a `Transaction` record (type=TRANSFER, status=SUCCESS)
- Persists a `Payment` record linked to the transaction — all in one `@Transactional` method

**Files generated:**
- `enums/PaymentMode.java` — UPI, NEFT, IMPS, RTGS
- `enums/PaymentStatus.java` — PENDING, SUCCESS, FAILED
- `entity/Payment.java`
- `dto/request/InitiatePaymentRequestDTO.java`
- `dto/response/PaymentResponseDTO.java`
- `constants/PaymentConstants.java`
- `repository/PaymentRepository.java`
- `mapper/PaymentMapper.java`
- `service/PaymentService.java`
- `service/impl/PaymentServiceImpl.java`
- `controller/PaymentController.java`

---

## Shared Infrastructure (generated once, used by all domains)

| File | Purpose |
|------|---------|
| `dto/ApiResponse.java` | Generic response wrapper — `success(message, data)` and `error(status, message)` |
| `dto/ErrorResponse.java` | Error response with optional field-level validation errors |
| `exception/AppException.java` | Base exception carrying `HttpStatus` |
| `exception/ResourceNotFoundException.java` | 404 — resource not found |
| `exception/DuplicateResourceException.java` | 409 — duplicate resource |
| `exception/BusinessException.java` | 422 — domain rule violation |
| `exception/handler/GlobalExceptionHandler.java` | `@RestControllerAdvice` handling all exception types uniformly |
| `src/main/resources/application.properties` | PostgreSQL datasource, HikariCP pool, JPA, virtual threads, Swagger |
| `build.gradle` | Gradle dependencies: Web, JPA, Validation, PostgreSQL, Lombok, MapStruct, SpringDoc |

---

## Standards Applied

- **Author comment** (`/** * author : Apurva Rawal **/`) above class annotations on every class
- **No BaseEntity** — audit fields (`createdAt`, `updatedAt`, `createdBy`, `updatedBy`) added directly to every entity
- **No Spring auditing annotations** — timestamps set via MapStruct `expression = "java(LocalDateTime.now())"`
- **DTOs are POJO classes** with `@Getter @Setter`, not Java records
- **Constants** are `interface` types (not `final class`)
- **`@Transactional`** only on write methods; no `readOnly` on reads
- **`@RequiredArgsConstructor`** for all dependency injection; no `@Autowired`
- **Pagination** via `Pageable` / `Page<T>` on all list endpoints
- **Enums** stored as `STRING`, placed in top-level `enums/` package with `@Getter` + `value` field
- **No `@Validated`** on controller classes
- **Create APIs** use `ResponseEntity.ok()` — not `ResponseEntity.status(201)`
- **SLF4J** entry/exit logging on all controller and service methods

---

## Git Commits

| Commit | Message |
|--------|---------|
| `70a42f6` | `feat(customer): scaffold project and generate Customer domain` |
| `ee7c9df` | `feat(account): generate Account domain` |
| `46e4697` | `feat(transaction): generate Transaction domain` |
| `4dd1af4` | `feat(payment): generate Payment domain` |

> Code has **not** been pushed to remote.
