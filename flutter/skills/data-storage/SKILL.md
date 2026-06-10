---

name: data-storage
description: Implement Secure Storage, Hive persistence, local datasources, offline support, preferences and data lifecycle management.
---------------------------------------------------------------------------------------------------------------------------------------

# Flutter Data Storage Standards

## When to use

Use this skill when implementing:

* Hive
* Flutter Secure Storage
* Local persistence
* User preferences
* Offline support
* Local datasources
* Cached data storage

## Related Skills

Use alongside:

* architecture
* api-standards
* auth-standards
* caching
* unit-testing

## Skill Usage

When implementing:

* Secure Storage
* Hive persistence
* Local datasources
* User preferences
* Offline support
* Cache persistence

follow every rule in this document.

Architecture, folder structure, dependency injection and repository structure must follow the architecture skill.

---

# Data Storage Standards

## 1. Storage Layers Overview

This project uses two distinct storage layers.

Never mix their responsibilities.

| Layer          | Package                | Use For                                              |
| -------------- | ---------------------- | ---------------------------------------------------- |
| Secure Storage | flutter_secure_storage | Tokens, PINs, biometric keys, sensitive credentials  |
| Local Database | hive + hive_flutter    | Cached API responses, user preferences, offline data |

---

## 2. Secure Storage

### Setup

Always use platform-specific secure configuration.

```dart
const FlutterSecureStorage storage =
    FlutterSecureStorage(
  aOptions: AndroidOptions(
    encryptedSharedPreferences: true,
  ),
  iOptions: IOSOptions(
    accessibility:
        KeychainAccessibility.first_unlock,
  ),
);
```

### Access Through Service

Always access storage through:

```text
lib/core/storage/secure_storage_service.dart
```

Never use FlutterSecureStorage directly inside features.

Correct:

```dart
final storage =
    ref.watch(
      secureStorageServiceProvider,
    );

await storage.saveToken(token);
```

Wrong:

```dart
const storage =
    FlutterSecureStorage();
```

---

### Reserved Keys

Define all keys in AppConstants.

```dart
AppConstants.tokenKey
AppConstants.refreshTokenKey
AppConstants.userIdKey
```

Rules:

* Never use raw keys.
* Never duplicate keys.
* Always use constants.

---

### SecureStorageService API

```dart
Future<void> saveToken(String token)
Future<String?> getToken()

Future<void> saveRefreshToken(
  String token,
)

Future<void> write(
  String key,
  String value,
)

Future<String?> read(String key)

Future<void> delete(String key)

Future<void> clearAll()
```

---

### Secure Storage Rules

* Clear all data on logout.
* Never store passwords.
* Never store OTPs.
* Never log secure values.
* Always handle null responses.

---

## 3. Hive Local Database

### Initialization

Initialize once before runApp.

```dart
void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  await Hive.initFlutter();

  runApp(
    const ProviderScope(
      child: App(),
    ),
  );
}
```

---

### Box Naming Convention

Define all box names in AppConstants.

```dart
static const String accountsBox =
    'accounts_box';

static const String transactionsBox =
    'transactions_box';

static const String userBox =
    'user_box';

static const String preferencesBox =
    'preferences_box';
```

Never use inline box names.

---

### Hive TypeAdapters

Every Hive model requires:

```dart
@HiveType()
```

and a generated adapter.

Example:

```dart
@HiveType(typeId: 1)
class AccountHiveModel
    extends HiveObject {
}
```

Generate adapters using build_runner.

---

### TypeId Registry

Never reuse typeIds.

Example registry:

| TypeId | Model                 |
| ------ | --------------------- |
| 0      | UserHiveModel         |
| 1      | AccountHiveModel      |
| 2      | TransactionHiveModel  |
| 3      | NotificationHiveModel |

---

### Adapter Registration

Register adapters before opening boxes.

```dart
Hive.registerAdapter(
  UserHiveModelAdapter(),
);
```

---

### Box Ownership

Each box must have a dedicated LocalDatasource.

Never open the same box in multiple locations.

Example:

```dart
AccountLocalDatasource
```

owns:

```dart
accounts_box
```

---

## 4. Local Datasource Pattern

Each feature must provide:

```dart
abstract class FeatureLocalDatasource
```

and

```dart
class FeatureLocalDatasourceImpl
```

Responsibilities:

* Hive access
* Local persistence
* Cache reads
* Cache writes

Repositories must never access Hive directly.

---

## 5. User Preferences

Store preferences in Hive.

Examples:

```dart
theme_mode
language
onboarding_complete
biometric_enabled
```

Never store:

```dart
token
refresh_token
password
pin
```

inside Hive.

---

## 6. Data Lifecycle

### Logout

On logout:

```dart
await secureStorage.clearAll();
```

Clear:

```dart
accounts_box
transactions_box
user_box
```

Preserve:

```dart
preferences_box
```

This preserves:

* Theme
* Language
* Onboarding state

---

### Cache Expiry

Store:

```dart
cachedAt
```

alongside cached data.

Rules:

* Transactional data → 15 minutes
* Profile data → 24 hours

Caching implementation must follow the caching skill.

---

## 7. Offline Support

Rules:

* Read cached data when offline.
* Sync fresh data when online.
* Persist important user state.
* Never persist sensitive credentials.

Offline strategies must follow the caching skill.

---

## 8. Folder Structure

```text
lib/
├── core/
│   └── storage/
│       └── secure_storage_service.dart
│
└── features/
    └── feature/
        └── data/
            ├── datasources/
            │   ├── feature_remote_datasource.dart
            │   └── feature_local_datasource.dart
            │
            └── models/
                ├── feature_model.dart
                └── feature_hive_model.dart
```

---

## 9. build_runner

After creating or modifying Hive models:

```bash
flutter pub run build_runner build --delete-conflicting-outputs
```

---

## 10. Do NOT

* Do not store JWTs in Hive.
* Do not store passwords in Hive.
* Do not store PINs in Hive.
* Do not reuse Hive typeIds.
* Do not use raw box names.
* Do not access Hive directly from repositories.
* Do not access Hive directly from UI.
* Do not clear preferences on logout.
* Do not store data without timestamps when caching.

---

## Output

Data storage implementations generated using this skill must:

* Follow Clean Architecture
* Use SecureStorageService
* Use Hive Local Datasources
* Use AppConstants for keys and box names
* Use generated TypeAdapters
* Support offline operation where appropriate
* Follow caching standards
* Include tests
* Follow the architecture skill

## Project Priorities

1. Security
2. Scalability
3. Data Integrity
4. Testability

All storage implementations must follow these priorities.
