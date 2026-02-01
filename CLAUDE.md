# Project Memory — spring-web-client

## Overview
Reusable Spring Boot library wrapping Spring WebClient with production-ready defaults. This is a JAR library consumed by applications like `spring-web-client-example-app`.

## Environment
- **JDK:** `C:\software\openjdk-25.0.2` (Java 25, compiles to target 21)
- **Maven:** `C:\software\apache-maven-3.9.12`
- **Build command:** `export JAVA_HOME="C:/software/openjdk-25.0.2" && "C:/software/apache-maven-3.9.12/bin/mvn" -f "C:/projects/spring-web-client/pom.xml" clean test`
- **Spring Boot:** 4.0.1
- **Java target:** 21

## Build Status (as of 2026-01-31)
- `mvn clean test` — **84 tests, 0 failures, BUILD SUCCESS**
- `mvn clean install` — JAR installed to local Maven repo

## Architecture

### Key Classes
| Class | Purpose |
|---|---|
| `ServiceClient` | Public API interface |
| `WebServiceClient` | Implementation with SRP methods: `resolveWebClient()`, `buildRequestSpec()`, `handleResponse()`, `applyRetry()`, `applyTimeout()` |
| `WebServiceRequest<T>` | Immutable request specification with builder pattern, including per-request filters |

### Features Implemented
1. **Per-request timeout/retry overrides** — `timeoutMs`, `maxRetries`, `retryIntervalMs` fields on `WebServiceRequest` (Integer/Long wrappers, null = use global)
2. **Per-request filter selection** — `filter(ExchangeFilterFunction)` on `WebServiceRequest` builder. Filters are NOT auto-registered on the shared WebClient. Each request declares which filters it needs. `WebServiceClient` applies them via `webClient.mutate()`.
3. **Retriable exception classification** — `RetriableExceptionPredicate`: 5xx/429/IOException = retriable; 4xx/RuntimeException = not retriable
4. **Retry logging** — WARN level with attempt number, max retries, exception type, message
5. **Three ExchangeFilterFunction beans** (registered as injectable Spring beans, NOT auto-applied):
   - `CorrelationIdFilterFunction` @Order(100) — adds X-Correlation-Id UUID
   - `BearerTokenFilterFunction` @Order(200) — injects Authorization header synchronously (no Scheduler)
   - `RequestLoggingFilterFunction` @Order(300) — DEBUG-level request/response logging
6. **SSL/mTLS** — `SslConnectionFactoryInitializer`, `AliasSelectingX509KeyManager`
7. **Connection pooling** — configurable via `webclient.http.pool.*`

### Removed Features
- `virtualThreadsEnabled` property and virtual-thread Scheduler — removed entirely
- `BearerTokenFilterFunction` no longer uses `Mono.fromCallable().subscribeOn()` — calls `getToken()` synchronously
- Filters are no longer auto-registered on the shared WebClient — moved to per-request via `WebServiceRequest.builder().filter()`

### Configuration Prefix
`webclient.http.*` — see `HttpClientProperties.java` for all nested groups (ssl, pool, timeout, retry)

### Auto-Configuration
`WebClientAutoConfiguration` registered via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

### Source Files (13 source, 10 test)
```
src/main/java/com/webclient/lib/
├── auth/BearerTokenFilterFunction.java, BearerTokenProvider.java
├── client/ServiceClient.java, WebServiceClient.java
├── config/HttpClientProperties.java, WebClientAutoConfiguration.java
├── filter/CorrelationIdFilterFunction.java, RequestLoggingFilterFunction.java
├── model/WebServiceRequest.java
├── retry/RetriableExceptionPredicate.java, RetryStrategyFactory.java
└── ssl/AliasSelectingX509KeyManager.java, SslConnectionFactoryInitializer.java
```

### Rename History
- Old directory: `web-client` → new: `spring-web-client`
- Old artifactId: `web-client-lib` → new: `spring-web-client`
- Old class names: `HttpRequestSpec`/`ReactiveHttpClient`/`ReactiveHttpClientImpl` → `WebServiceRequest`/`ServiceClient`/`WebServiceClient`

## Companion Project
The example-app lives at `C:\projects\spring-web-client-example-app` (separate Maven project, NOT inside spring-web-client). See its own `CLAUDE.md`.
