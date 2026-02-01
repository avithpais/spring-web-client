# spring-web-client

A reusable Spring Boot library that wraps Spring `WebClient` with production-ready defaults: connection pooling, mutual TLS, automatic retry with exponential backoff, retriable/non-retriable exception classification, per-request filter selection, per-request timeout/retry overrides, and request correlation/logging filters.

## Requirements

- Java 21+
- Spring Boot 4.0+

## Installation

Build the library JAR:

```bash
mvn clean install
```

Add the dependency to your application's `pom.xml`:

```xml
<dependency>
    <groupId>com.webclient</groupId>
    <artifactId>spring-web-client</artifactId>
    <version>1.0.0</version>
</dependency>
```

The library uses Spring Boot auto-configuration. Adding it to the classpath is enough — all beans are registered automatically.

## Quick Start

Inject `ServiceClient` and the filters you need, then build a `WebServiceRequest`:

```java
@Service
public class MyService {

    @Autowired private ServiceClient serviceClient;
    @Autowired private CorrelationIdFilterFunction correlationIdFilter;
    @Autowired private BearerTokenFilterFunction bearerTokenFilter;

    public Mono<MyResponse> callDownstream() {
        WebServiceRequest<MyResponse> request = WebServiceRequest.<MyResponse>builder()
                .url("https://api.example.com/data")
                .method(HttpMethod.GET)
                .acceptType(MediaType.APPLICATION_JSON)
                .responseType(MyResponse.class)
                .filter(correlationIdFilter)
                .filter(bearerTokenFilter)
                .build();

        return serviceClient.execute(request);
    }
}
```

## Configuration

All properties use the prefix `webclient.http` in `application.properties` or `application.yml`.

### Connection Pool

| Property | Default | Description |
|---|---|---|
| `webclient.http.pool.max-connections` | `500` | Maximum total connections in the pool |
| `webclient.http.pool.max-connections-per-route` | `50` | Maximum connections per host |

### Timeouts

| Property | Default | Description |
|---|---|---|
| `webclient.http.timeout.connect-request-timeout-ms` | `5000` | TCP connect timeout (ms) |
| `webclient.http.timeout.timeout-ms` | `30000` | Response timeout (ms) — global default |

### Retry

| Property | Default | Description |
|---|---|---|
| `webclient.http.retry.max-retries` | `3` | Maximum retry attempts (exponential backoff) |
| `webclient.http.retry.retry-interval-ms` | `1000` | Initial backoff interval (ms) |

Retry automatically classifies exceptions:
- **Retriable** (will be retried): HTTP 5xx, HTTP 429, `WebClientRequestException`, `IOException`, `ConnectException`
- **Non-retriable** (fail immediately): HTTP 4xx (except 429), `RuntimeException`, `IllegalArgumentException`, etc.

Each retry attempt is logged at WARN level with the attempt number, max retries, exception type, and message.

### SSL / mTLS

| Property | Default | Description |
|---|---|---|
| `webclient.http.ssl.enabled` | `false` | Enable SSL/TLS |
| `webclient.http.ssl.keystore-path` | | Path to PKCS12 keystore |
| `webclient.http.ssl.keystore-password` | | Keystore password |
| `webclient.http.ssl.keystore-type` | `PKCS12` | Keystore type |
| `webclient.http.ssl.truststore-path` | | Path to PKCS12 truststore |
| `webclient.http.ssl.truststore-password` | | Truststore password |
| `webclient.http.ssl.truststore-type` | `PKCS12` | Truststore type |
| `webclient.http.ssl.key-alias-name` | | Alias to select from keystore |
| `webclient.http.ssl.bypass-verification` | `false` | Skip certificate verification (non-production only) |
| `webclient.http.ssl.tls-protocol` | `TLSv1.2` | TLS protocol version |

## Per-Request Filter Selection

Filters are **not** auto-registered on the shared `WebClient`. Instead, each `WebServiceRequest` declares which filters it needs via the builder's `filter()` method. This gives callers full control — for example, bearer-token injection may only be needed for certain downstream services.

The library auto-configures three `ExchangeFilterFunction` beans as injectable Spring beans:

| Order | Bean | Description |
|---|---|---|
| 100 | `CorrelationIdFilterFunction` | Adds `X-Correlation-Id` UUID header if not already present |
| 200 | `BearerTokenFilterFunction` | Injects `Authorization: Bearer <token>` from a `BearerTokenProvider` bean |
| 300 | `RequestLoggingFilterFunction` | Logs request/response at DEBUG level with method, URL, status, and elapsed time |

All three are `@ConditionalOnMissingBean` — you can replace any by registering your own bean of the same type. You can also create additional custom `ExchangeFilterFunction` beans.

### Usage

```java
@Autowired private BearerTokenFilterFunction bearerTokenFilter;
@Autowired private CorrelationIdFilterFunction correlationIdFilter;
@Autowired private RequestLoggingFilterFunction loggingFilter;

// Authenticated call — all three filters
WebServiceRequest.<Post>builder()
        .url("https://internal-api/posts/1")
        .responseType(Post.class)
        .filter(correlationIdFilter)
        .filter(bearerTokenFilter)
        .filter(loggingFilter)
        .build();

// Public call — no bearer token needed
WebServiceRequest.<String>builder()
        .url("https://public-api/health")
        .responseType(String.class)
        .filter(correlationIdFilter)
        .filter(loggingFilter)
        .build();

// Minimal call — no filters at all
WebServiceRequest.<String>builder()
        .url("https://simple-endpoint/ping")
        .responseType(String.class)
        .build();
```

When a request has filters, `WebServiceClient` applies them via `webClient.mutate()`, creating a lightweight per-request variant. When no filters are specified, the base `WebClient` is used directly with zero overhead.

## Per-Request Timeout and Retry Overrides

Global timeout/retry settings serve as defaults. Individual requests can override them:

```java
WebServiceRequest<MyResponse> request = WebServiceRequest.<MyResponse>builder()
        .url("https://slow-service.example.com/heavy")
        .method(HttpMethod.GET)
        .responseType(MyResponse.class)
        .timeoutMs(60000)        // 60s total deadline (global default: 30s)
        .maxRetries(5)           // 5 retries (global default: 3)
        .retryIntervalMs(2000)   // 2s initial backoff (global default: 1s)
        .build();
```

| Field | Type | Default | Behavior |
|---|---|---|---|
| `timeoutMs` | `Integer` | `null` (use global) | Total deadline for the entire operation including retries |
| `maxRetries` | `Integer` | `null` (use global) | Max retry attempts. `0` = no retry at all |
| `retryIntervalMs` | `Long` | `null` (use global) | Initial backoff interval for exponential retry |

Setting `maxRetries(0)` skips the retry operator entirely — useful for non-idempotent calls or when the caller handles retries externally.

The timeout is applied **after** the retry operator, so it acts as a total deadline for the entire operation (all attempts combined), not per-attempt.

### Override Combinations

| `maxRetries` | `retryIntervalMs` | Behavior |
|---|---|---|
| `null` | `null` | Global retry spec used |
| `null` | set | Global `maxRetries` + per-request interval |
| set (> 0) | `null` | Per-request retries + global interval |
| set (> 0) | set | Fully per-request retry spec |
| `0` | any | No retry — `.retryWhen()` is skipped |

## Bearer Token Injection

The `BearerTokenFilterFunction` injects an `Authorization: Bearer <token>` header when attached to a request. It calls `BearerTokenProvider.getToken()` synchronously — implement thread-safe caching in your provider (e.g., using `StampedLock`).

Register a provider bean in your application:

```java
@Bean
public BearerTokenProvider bearerTokenProvider() {
    return () -> myTokenCache.getOrRefreshToken();
}
```

The filter only runs on requests that include it via `.filter(bearerTokenFilter)`. If the request already contains an `Authorization` header, the filter preserves it. If no `BearerTokenProvider` bean is registered, the filter is a no-op.

## API Aggregator Pattern

The library is designed for services that fan out to multiple downstream APIs, aggregate responses, and return a single result. Use `Mono.zip()` for parallel calls with independent timeout budgets and filter sets:

```java
public Mono<AggregatedResponse> aggregate(long id) {
    Mono<UserProfile> user = serviceClient.execute(
            WebServiceRequest.<UserProfile>builder()
                    .url("https://user-service/users/" + id)
                    .responseType(UserProfile.class)
                    .filter(correlationIdFilter)
                    .filter(bearerTokenFilter)
                    .timeoutMs(3000)
                    .maxRetries(1)
                    .build());

    Mono<OrderHistory> orders = serviceClient.execute(
            WebServiceRequest.<OrderHistory>builder()
                    .url("https://order-service/orders?userId=" + id)
                    .responseType(OrderHistory.class)
                    .filter(correlationIdFilter)
                    .filter(bearerTokenFilter)
                    .timeoutMs(10000)
                    .maxRetries(3)
                    .retryIntervalMs(500)
                    .build());

    return Mono.zip(user, orders)
            .map(tuple -> new AggregatedResponse(tuple.getT1(), tuple.getT2()));
}
```

For chained calls where one response feeds the next:

```java
public Mono<EnrichedOrder> getEnrichedOrder(long orderId) {
    return serviceClient.execute(
            WebServiceRequest.<Order>builder()
                    .url("https://order-service/orders/" + orderId)
                    .responseType(Order.class)
                    .filter(correlationIdFilter)
                    .filter(bearerTokenFilter)
                    .timeoutMs(5000)
                    .build())
            .flatMap(order -> serviceClient.execute(
                    WebServiceRequest.<Product>builder()
                            .url("https://product-service/products/" + order.getProductId())
                            .responseType(Product.class)
                            .filter(correlationIdFilter)
                            .timeoutMs(3000)
                            .maxRetries(2)
                            .build())
                    .map(product -> new EnrichedOrder(order, product)));
}
```

## Architecture

```
┌──────────────────────────────────────────────────────────────┐
│  Your Application                                            │
│                                                              │
│  @Autowired ServiceClient serviceClient;                     │
│  @Autowired BearerTokenFilterFunction bearerTokenFilter;     │
│  @Autowired CorrelationIdFilterFunction correlationIdFilter;  │
│                                                              │
│  WebServiceRequest.builder()                                 │
│      .url(...)                                               │
│      .filter(correlationIdFilter)   // ← per-request         │
│      .filter(bearerTokenFilter)     // ← per-request         │
│      .build()                                                │
│  serviceClient.execute(request) → Mono<T>                    │
└───────────────────────┬──────────────────────────────────────┘
                        │
┌───────────────────────▼──────────────────────────────────────┐
│  spring-web-client (auto-configured)                            │
│                                                              │
│  WebServiceClient                                            │
│    ├── resolveWebClient() — applies per-request filters      │
│    │                        via webClient.mutate()            │
│    ├── buildRequestSpec() — builds WebClient request         │
│    ├── handleResponse()   — deserializes response body       │
│    ├── applyRetry()       — per-request or global retry      │
│    └── applyTimeout()     — per-request or global timeout    │
│                                                              │
│  ExchangeFilterFunction beans (injectable, per-request):     │
│    ├── CorrelationIdFilterFunction  @Order(100)              │
│    ├── BearerTokenFilterFunction    @Order(200)              │
│    └── RequestLoggingFilterFunction @Order(300)              │
│                                                              │
│  RetryStrategyFactory                                        │
│    ├── creates Retry.backoff() specs                         │
│    ├── filters via RetriableExceptionPredicate               │
│    └── logs retry attempts at WARN level                     │
│                                                              │
│  SslConnectionFactoryInitializer                             │
│    └── builds Netty SslContext (mTLS, truststore, alias)     │
│                                                              │
│  WebClientAutoConfiguration                                  │
│    └── wires ConnectionProvider, HttpClient, WebClient       │
└──────────────────────────────────────────────────────────────┘
```

## Testing

```bash
mvn clean test
```

84 tests covering:
- `WebServiceRequest` builder validation, immutability, and per-request filter list
- `RetryStrategyFactory` retry counting with global and explicit parameters
- `RetriableExceptionPredicate` classification of retriable vs non-retriable exceptions
- `WebServiceClient` request execution, retry behavior, per-request timeout/retry overrides, per-request filter application via `webClient.mutate()`
- `BearerTokenFilterFunction` token injection and skip-when-present logic
- `CorrelationIdFilterFunction` header injection and preservation
- `RequestLoggingFilterFunction` passthrough and error propagation
- `HttpClientProperties` defaults and binding
- `AliasSelectingX509KeyManager` and `SslConnectionFactoryInitializer` SSL configuration

## Project Structure

```
src/main/java/com/webclient/lib/
├── auth/
│   ├── BearerTokenFilterFunction.java      # @Order(200) injectable filter for token injection
│   └── BearerTokenProvider.java            # Functional interface for token retrieval
├── client/
│   ├── ServiceClient.java                  # Public API interface
│   └── WebServiceClient.java              # Implementation with SRP methods
├── config/
│   ├── HttpClientProperties.java           # @ConfigurationProperties binding
│   └── WebClientAutoConfiguration.java     # Auto-configuration entry point
├── filter/
│   ├── CorrelationIdFilterFunction.java    # @Order(100) injectable filter for X-Correlation-Id
│   └── RequestLoggingFilterFunction.java   # @Order(300) injectable filter for DEBUG logging
├── model/
│   └── WebServiceRequest.java             # Immutable request spec (builder, filters, overrides)
├── retry/
│   ├── RetriableExceptionPredicate.java    # Classifies retriable vs non-retriable
│   └── RetryStrategyFactory.java           # Creates Reactor Retry specs with logging
└── ssl/
    ├── AliasSelectingX509KeyManager.java   # Selects a specific key alias from keystore
    └── SslConnectionFactoryInitializer.java # Builds Netty SslContext
```

## License

Internal library — see your organization's licensing policy.
