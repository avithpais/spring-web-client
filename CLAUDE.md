# Project Memory — spring-web-client

## Overview
Reusable Spring Boot library wrapping Spring WebClient (reactive) and RestClient (synchronous) with production-ready defaults. This is a JAR library consumed by applications like `spring-web-client-example-app`.

## Environment
- **JDK:** `C:\software\openjdk-25.0.2` (Java 25, compiles to target 21)
- **Maven:** `C:\software\apache-maven-3.9.12`
- **Build command:** `export JAVA_HOME="C:/software/openjdk-25.0.2" && "C:/software/apache-maven-3.9.12/bin/mvn" -f "C:/projects/spring-web-client/pom.xml" clean test`
- **Spring Boot:** 4.0.1
- **Java target:** 21

## Build Status (as of 2026-02-07)
- `mvn clean test` — **127 tests, 0 failures, BUILD SUCCESS**
- `mvn clean install` — JAR installed to local Maven repo

## Architecture

### Key Classes
| Class | Purpose |
|---|---|
| `WebServiceClient` | Reactive HTTP client (WebClient-based), returns `Mono<T>` |
| `RestServiceClient` | Synchronous HTTP client (RestClient-based), returns `T` directly |
| `WebServiceRequest<T>` | Immutable request spec with builder pattern, per-request filters |
| `RestServiceRequest<T>` | Immutable request spec with builder pattern, per-request interceptors |

### Features Implemented
1. **Per-request timeout/retry overrides** — `timeoutMs`, `maxRetries`, `retryIntervalMs` fields on request builders
2. **Per-request filter/interceptor selection** — Filters are NOT auto-registered. Each request declares which it needs via `filter()` or `interceptor()` builder methods
3. **Retriable exception classification** — 5xx/429/IOException = retriable; 4xx/RuntimeException = not retriable
4. **Shared HTTP infrastructure** — Both WebClient and RestClient share the same Netty HttpClient, SSL context, and connection pool
5. **Filter beans (WebClient):**
   - `CorrelationIdFilterFunction` @Order(100) — adds X-Correlation-Id UUID
   - `BearerTokenFilterFunction` @Order(200) — injects Authorization header
   - `RequestLoggingFilterFunction` @Order(300) — DEBUG-level logging
6. **Interceptor beans (RestClient):**
   - `CorrelationIdInterceptor` @Order(100)
   - `BearerTokenInterceptor` @Order(200)
   - `RequestLoggingInterceptor` @Order(300)
7. **SSL/mTLS** — `SslConnectionFactoryInitializer`, `AliasSelectingX509KeyManager`
8. **Connection pooling** — configurable via `webclient.http.pool.*`

### Shared Constants
`util/HttpHeaders.java` — common header names (`CORRELATION_ID`, `AUTHORIZATION`)

### Configuration Prefix
`webclient.http.*` — see `HttpClientProperties.java` for all nested groups (ssl, pool, timeout, retry)

### Auto-Configuration
- `WebClientAutoConfiguration` — WebClient, RestClient, filters, infrastructure
- `RestClientAutoConfiguration` — RestClient interceptors, SyncRetryExecutor

### Source Files (21 source, 15 test)
```
src/main/java/com/webclient/lib/
├── auth/BearerTokenFilterFunction.java, BearerTokenInterceptor.java, BearerTokenProvider.java
├── client/WebServiceClient.java, RestServiceClient.java
├── config/HttpClientProperties.java, WebClientAutoConfiguration.java, RestClientAutoConfiguration.java
├── filter/CorrelationIdFilterFunction.java, RequestLoggingFilterFunction.java
├── interceptor/CorrelationIdInterceptor.java, RequestLoggingInterceptor.java
├── model/WebServiceRequest.java, RestServiceRequest.java
├── retry/RetriableExceptionPredicate.java, SyncRetriableExceptionPredicate.java, RetryStrategyFactory.java, SyncRetryExecutor.java
├── ssl/AliasSelectingX509KeyManager.java, SslConnectionFactoryInitializer.java
└── util/HttpHeaders.java
```

## Companion Project
The example-app lives at `C:\projects\spring-web-client-example-app` (separate Maven project). See its own `CLAUDE.md`.
