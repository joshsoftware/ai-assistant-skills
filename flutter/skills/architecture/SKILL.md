---

name: architecture
description: Define and enforce Flutter application architecture, folder structure, dependency rules, state management, navigation, networking, storage, testing, and feature generation standards.
---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

# Flutter Architecture & Tech Stack Guide

**Audience:** Flutter engineers building and maintaining production-grade applications.

---

# Architecture Principles

| Principle          | Description                                                           |
| ------------------ | --------------------------------------------------------------------- |
| Clean Architecture | Business rules live in Domain. UI and frameworks remain at the edges. |
| Feature First      | Organize code by business capability, not technical type.             |
| Layer Separation   | Presentation, Domain and Data must remain separate.                   |
| Dependency Rule    | Dependencies point inward only.                                       |
| Testability        | Every layer must be independently testable.                           |

---

# Layer Responsibilities

```text
Presentation
     ↓
Domain
     ↓
Data
```

## Presentation Layer

Contains:

* Screens
* Widgets
* Providers
* State Management

Responsibilities:

* Render UI
* Manage state
* Handle user interactions

Must NOT contain:

* Dio calls
* Hive operations
* Business rules
* JSON parsing

---

## Domain Layer

Contains:

* Entities
* Repository Contracts
* UseCases

Responsibilities:

* Business rules
* Application rules
* Authorization logic

Must NOT contain:

* Flutter imports
* Dio imports
* Hive imports
* Widgets
* BuildContext

---

## Data Layer

Contains:

* Datasources
* Models
* Repository Implementations

Responsibilities:

* API communication
* Local storage
* Data mapping
* Error mapping

Must NOT contain:

* UI state
* Navigation
* Theme logic

---

# Feature First Structure

Each feature must be self-contained.

```text
lib/
├── app/
│
├── core/
│
├── shared/
│
└── features/
    └── customer/
        ├── domain/
        │   ├── entities/
        │   ├── repositories/
        │   └── usecases/
        │
        ├── data/
        │   ├── datasources/
        │   ├── models/
        │   └── repositories/
        │
        └── presentation/
            ├── providers/
            ├── screens/
            └── widgets/
```

---

# Core Application Structure

```text
lib/
├── main.dart
│
├── app/
│   ├── app.dart
│   ├── router/
│   └── theme/
│
├── core/
│   ├── auth/
│   ├── cache/
│   ├── constants/
│   ├── error/
│   ├── network/
│   ├── storage/
│   └── services/
│
├── shared/
│   ├── widgets/
│   ├── extensions/
│   └── utils/
│
└── features/
```

---

# Data Flow

```text
UI
 ↓
Notifier
 ↓
UseCase
 ↓
Repository
 ↓
Datasource
 ↓
ApiClient / Hive
```

Rules:

* UI never calls API directly.
* UI never accesses storage directly.
* UseCases communicate through repositories.
* Datasources communicate through infrastructure.

---

# State Management

Use:

* Riverpod
* StateNotifierProvider

Example:

```dart
final customerNotifierProvider =
    StateNotifierProvider<CustomerNotifier, CustomerState>(
  (ref) => CustomerNotifier(
    ref.watch(getCustomersUsecaseProvider),
  ),
);
```

Rules:

* One provider per feature.
* No global mutable state.
* Use dependency injection through Riverpod.

---

# Dependency Injection

Use Riverpod Providers.

Example:

```dart
final customerRepositoryProvider =
    Provider<CustomerRepository>(
  (ref) => CustomerRepositoryImpl(
    ref.watch(apiClientProvider),
  ),
);
```

Rules:

* Never instantiate repositories in screens.
* Never instantiate datasources in screens.
* All dependencies must come from providers.

---

# Navigation

Use:

* GoRouter

Location:

```text
lib/app/router/
```

Rules:

* Centralized routing.
* Route guards for authentication.
* No direct Navigator.push usage.

---

# Networking

Use:

* Dio
* ApiClient

Location:

```text
lib/core/network/
```

Rules:

* Features must never use Dio directly.
* Use centralized ApiClient.
* Configure interceptors globally.
* JWT handled centrally.

---

# Local Storage

Use:

* Hive
* Flutter Secure Storage

Rules:

### Hive

Store:

* Cache
* Preferences
* Offline data

Do NOT store:

* JWT
* Refresh Tokens
* Credentials

### Secure Storage

Store:

* JWT
* Refresh Tokens
* Sensitive data

---

# Security

Required:

* JWT Authentication
* Token Refresh
* Secure Storage
* Session Timeout
* Biometric Authentication Support

Rules:

* Never log credentials.
* Never log tokens.
* Never store sensitive data in Hive.

---

# Error Handling

Use:

```dart
Either<Failure, T>
```

Rules:

* Repositories return Either.
* UI never handles raw exceptions.
* Convert exceptions to Failures.

---

# Shared Components

Reusable widgets belong in:

```text
lib/shared/widgets/
```

Examples:

* AppButton
* AppTextField
* LoadingIndicator
* ErrorView
* EmptyStateView

Feature-specific widgets stay inside feature folders.

---

# Naming Conventions

## Files

```text
customer_repository.dart
customer_repository_impl.dart
customer_provider.dart
customer_screen.dart
customer_usecase.dart
```

## Classes

```dart
CustomerRepository
CustomerRepositoryImpl
CustomerNotifier
CustomerScreen
GetCustomersUsecase
```

---

# Testing Standards

Mirror production structure:

```text
test/
└── features/
    └── customer/
```

Required:

* UseCase Tests
* Repository Tests
* Provider Tests
* Widget Tests

---

# Approved Tech Stack

| Category          | Technology             |
| ----------------- | ---------------------- |
| State Management  | Riverpod               |
| Navigation        | GoRouter               |
| Networking        | Dio                    |
| Storage           | Hive                   |
| Secure Storage    | Flutter Secure Storage |
| Biometrics        | local_auth             |
| Testing           | flutter_test, mockito  |
| Equality          | equatable              |
| Functional Errors | dartz                  |

---

# Related Skills

Use additional skills when required:

* api-standards
* auth-standards
* data-storage
* caching
* unit-testing
* flutter-pr-review

---

# Project Priorities

1. Security
2. Scalability
3. Clean Architecture
4. Testability

---

# Output

All generated features must:

* Follow Clean Architecture.
* Follow Feature First Structure.
* Use Riverpod.
* Use GoRouter.
* Use Dio through ApiClient.
* Use approved storage mechanisms.
* Follow dependency rules.
* Include test structure.

This file is the canonical architecture specification for all Flutter projects using this framework.
