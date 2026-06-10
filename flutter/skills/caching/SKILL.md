---

name: caching
description: Implement cache-first repositories, offline support, TTL management, cache invalidation and cache warm-up strategies.
----------------------------------------------------------------------------------------------------------------------------------

# Flutter Caching Standards

## When to use

Use this skill when implementing:

* Offline support
* Repository caching
* Cache invalidation
* TTL implementation
* Cache warm-up
* Background refresh
* Paginated cache
* API response caching

## Related Skills

Use alongside:

* architecture
* api-standards
* data-storage
* unit-testing

## Skill Usage

When implementing:

* API response caching
* Offline support
* TTL management
* Cache invalidation
* Cache warm-up
* Paginated cache
* Background refresh

follow every rule in this document.

Architecture, folder structure, repository structure and dependency injection must follow the architecture skill.

---

# Caching Standards

## 1. Caching Philosophy

This project follows a **Cache-First with Background Refresh** approach for read-heavy application data.

| Strategy      | When to Use                                            |
| ------------- | ------------------------------------------------------ |
| Cache-First   | Profile, dashboard, transaction history, account lists |
| Network-First | Time-sensitive and frequently changing data            |
| Cache-Only    | Preferences, configuration                             |
| Network-Only  | Authentication, login, logout, token refresh           |

Never apply caching to write operations (POST/PUT/DELETE).

---

[KEEP ALL EXISTING SECTIONS FROM YOUR CURRENT FILE UNCHANGED]

* Cache Entry Structure
* TTL Rules
* Cache Key Convention
* CacheService
* Repository Caching Pattern
* Network-First Strategy
* Cache Invalidation Rules
* Paginated Cache
* Offline Detection
* UI State for Cached Data
* Cache Warm-Up on Login
* Folder Structure
* Do NOT

---

## Output

Caching implementations generated using this skill must:

* Follow Clean Architecture
* Follow Repository Pattern
* Use CacheService as the single cache abstraction
* Use Hive for cache persistence
* Use CacheConstants for TTL management
* Use CacheKeys for key generation
* Include cache invalidation strategies
* Include offline support where applicable
* Include cache warm-up where appropriate
* Follow the architecture skill

## Project Priorities

1. Security
2. Scalability
3. Performance
4. Testability

All cache implementations must follow these priorities.
