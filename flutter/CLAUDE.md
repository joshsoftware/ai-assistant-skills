# Flutter AI Project Instructions

Before making any code changes:

1. Determine the type of work.
2. Load the required skills.
3. Follow all standards from those skills.
4. If multiple skills apply, combine them.
5. Do not ask the user which skill to use.

---

## Skill Loading

When a request matches a category below:

1. Load the corresponding skill(s).
2. Follow all standards from those skills.
3. If multiple skills apply, combine them.
4. Prefer existing project patterns over generating new patterns.

---

### Project Initialization

Load:

- project-bootstrap

Trigger when user requests:

- Initialize this project
- Setup this project
- Set up this project
- Bootstrap this project
- Configure project foundation
- Create project structure
- Prepare project architecture

### New Feature

Load:

* architecture

The architecture skill may invoke:

* api-standards
* auth-standards
* data-storage
* caching
* unit-testing

when required by the feature.

Examples:

* Create Login feature
* Create Customer feature
* Create Dashboard feature
* Create Notification feature

---

### Existing Feature API Integration

Load:

* architecture
* api-standards

Use only when APIs are being added to an existing feature.

Examples:

* Add API integration to Customer feature
* Integrate Dashboard with backend APIs

---

### Authentication

Load:

* architecture
* auth-standards
* data-storage
* unit-testing

Examples:

* Implement login
* Implement forgot password
* Add biometric authentication
* Add session timeout

---

### Local Storage

Load:

* data-storage

Examples:

* Store customer profile locally
* Persist user preferences
* Create Hive storage

---

### Caching

Load:

* caching

Examples:

* Add caching to customer list
* Implement cache invalidation
* Add offline support

---

### Testing

Load:

* unit-testing

Examples:

* Generate tests for Customer feature
* Add repository tests
* Add provider tests

---

### Pull Request Review

Load:

* flutter-pr-review

Examples:

* Review current branch changes
* Review this pull request
* Perform pre-merge validation

---

## Project Priorities

1. Security
2. Scalability
3. Clean Architecture
4. Testability
5. Maintainability

---

## Technology Stack

* Flutter
* Riverpod
* Dio
* GoRouter
* Hive
* Flutter Secure Storage
* local_auth

---

## Architecture Rules

* Follow Clean Architecture.
* Follow Feature First Structure.
* Use Repository Pattern.
* Use Riverpod for dependency injection.
* Use GoRouter for navigation.
* Use ApiClient for networking.
* Use SecureStorageService for secure data.
* Use Hive only through Local Datasources.

Never bypass architecture rules.

---

## Output Expectations

Generated code must:

* Be production ready.
* Follow project standards.
* Include proper error handling.
* Follow null safety.
* Include tests where applicable.
* Follow the architecture skill as the source of truth.
